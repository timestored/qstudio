/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2023 TimeStored
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.timestored.rayforce;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Test;

import kx.c.Flip;
import kx.c.KException;

/**
 * Drives {@link RayIpc} against a socket that speaks the Rayforce wire format by
 * hand, so the handshake, framing and deserializer are all checked without a
 * Rayforce install. The encoder here is written from the format in rayforce
 * src/store/serde.c - if it and RayIpc ever disagree, one of them is wrong.
 */
public class RayIpcTest {

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final int PREFIX = 0xcefadefa;
	private static final byte WIRE_VERSION = 3;

	private FakeServer server;

	@After public void tearDown() throws IOException {
		if (server != null) {
			server.close();
		}
	}

	@Test public void nullByteAtomStillConsumesItsValueByte() throws Exception {
		server = new FakeServer(false, null, frame(list(u8Atom(0x7f, true), i64Atom(42)), false));
		RayIpc ipc = connect(null, null);
		Object[] got = (Object[]) ipc.query("[0Nu 42]").getValue();
		assertEquals(Byte.valueOf((byte) 0), got[0]);
		assertEquals(Long.valueOf(42), got[1]);
		ipc.close();
	}

	// ------------------------------------------------------------ handshake

	@Test public void connectsToAnOpenServer() throws Exception {
		server = new FakeServer(false, null, frame(i64Atom(3), false));
		RayIpc ipc = connect(null, null);
		assertEquals(Long.valueOf(3), ipc.query("(+ 1 2)").getValue());
		assertEquals("", ipc.query("(+ 1 2)").getConsole());
		ipc.close();
	}

	@Test public void sendsCredentialsWhenTheServerAsksForThem() throws Exception {
		server = new FakeServer(true, null, frame(i64Atom(7), false));
		RayIpc ipc = connect("alice", "s3cret");
		assertEquals(Long.valueOf(7), ipc.query("1").getValue());
		ipc.close();
		assertEquals("alice:s3cret", server.credentialsSeen.get());
	}

	@Test public void rejectsAWireVersionItCannotSpeak() throws Exception {
		server = new FakeServer(false, (byte) 99, frame(i64Atom(1), false));
		try {
			connect(null, null);
			fail("expected a wire version mismatch");
		} catch (KException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("wire version"));
		}
	}

	@Test public void reportsARejectedPassword() throws Exception {
		server = new FakeServer(true, null, frame(i64Atom(1), false));
		server.rejectAuth = true;
		try {
			connect("alice", "wrong");
			fail("expected an authentication failure");
		} catch (KException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("authentication"));
		}
	}

	/** The query goes out as a STR atom carrying the source text, nothing else. */
	@Test public void sendsTheQueryAsAStringAtom() throws Exception {
		server = new FakeServer(false, null, frame(i64Atom(1), false));
		RayIpc ipc = connect(null, null);
		ipc.query("(count trades)");
		ipc.close();

		ByteBuffer b = ByteBuffer.wrap(server.requestSeen.get()).order(ByteOrder.LITTLE_ENDIAN);
		assertEquals(PREFIX, b.getInt());
		assertEquals(WIRE_VERSION, b.get());
		assertEquals("verbose flag set", 0x04, b.get() & 0x04);
		assertEquals("little endian payload", 0, b.get());
		assertEquals("sync message", 1, b.get());
		assertEquals(1 + 1 + 8 + "(count trades)".length(), b.getLong());
		assertEquals((byte) -13, b.get()); // STR atom
		b.get(); // flags
		assertEquals("(count trades)".length(), b.getLong());
		byte[] text = new byte[14];
		b.get(text);
		assertEquals("(count trades)", new String(text, UTF8));
	}

	// -------------------------------------------------------------- decoding

	@Test public void decodesATable() throws Exception {
		byte[] table = table(new String[] { "sym", "qty" },
				new byte[][] { symVec("AAPL", "GOOG"), i64Vec(100, 200) });
		server = new FakeServer(false, null, frame(table, false));

		RayIpc ipc = connect(null, null);
		Object o = ipc.query("trades").getValue();
		ipc.close();

		assertTrue(String.valueOf(o), o instanceof Flip);
		Flip f = (Flip) o;
		assertArrayEquals(new String[] { "sym", "qty" }, f.x);
		assertArrayEquals(new String[] { "AAPL", "GOOG" }, (String[]) f.at("sym"));
		assertArrayEquals(new long[] { 100, 200 }, (long[]) f.at("qty"));
	}

	/** Days since 2000-01-01 on the wire, the same epoch kdb uses. */
	@Test public void decodesDatesAgainstTheYear2000Epoch() throws Exception {
		ByteArrayOutputStream v = new ByteArrayOutputStream();
		v.write(8); // DATE vector
		v.write(0); // attrs
		v.write(le64(2));
		v.write(le32(8780)); // 2024-01-15
		v.write(le32(8781));
		server = new FakeServer(false, null, frame(v.toByteArray(), false));

		RayIpc ipc = connect(null, null);
		LocalDate[] d = (LocalDate[]) ipc.query("dates").getValue();
		ipc.close();
		assertEquals(LocalDate.of(2024, 1, 15), d[0]);
		assertEquals(LocalDate.of(2024, 1, 16), d[1]);
	}

	/** A typed null still carries its value bytes; skipping them desyncs the frame. */
	@Test public void keepsItsPlaceAcrossATypedNull() throws Exception {
		ByteArrayOutputStream list = new ByteArrayOutputStream();
		list.write(0); // LIST
		list.write(0); // attrs
		list.write(le64(2));
		list.write(new byte[] { (byte) -5, 1, 0, 0, 0, 0, 0, 0, 0, 0 }); // null i64
		list.write(i64Atom(42));
		server = new FakeServer(false, null, frame(list.toByteArray(), false));

		RayIpc ipc = connect(null, null);
		Object[] r = (Object[]) ipc.query("(list 0N 42)").getValue();
		ipc.close();
		assertEquals(Long.valueOf(Long.MIN_VALUE), r[0]);
		assertEquals(Long.valueOf(42), r[1]);
	}

	/** Anything over 2000 bytes arrives run-length encoded and delta coded. */
	@Test public void decodesACompressedFrame() throws Exception {
		long[] values = new long[400];
		for (int i = 0; i < values.length; i++) {
			values[i] = i;
		}
		byte[] payload = i64Vec(values);
		assertTrue("payload should exceed the compression threshold", payload.length > 2000);

		server = new FakeServer(false, null, frame(payload, true));
		RayIpc ipc = connect(null, null);
		long[] got = (long[]) ipc.query("(til 400)").getValue();
		ipc.close();
		assertArrayEquals(values, got);
	}

	@Test public void decompressRoundTripsWhatTheServerWouldSend() throws Exception {
		byte[] original = new byte[512];
		for (int i = 0; i < original.length; i++) {
			original[i] = (byte) (i < 300 ? 7 : i); // a long run, then literals
		}
		byte[] wire = compress(original);
		assertArrayEquals(original, RayIpc.decompress(wire, original.length));
	}

	// ---------------------------------------------------------------- errors

	@Test public void raisesTheServersErrorWithItsDetail() throws Exception {
		ByteArrayOutputStream pair = new ByteArrayOutputStream();
		pair.write(0); // LIST
		pair.write(0); // attrs
		pair.write(le64(2));
		pair.write(strAtom("error: type: add: expects a numeric argument, got str\n"));
		pair.write(errorObject("type"));
		server = new FakeServer(false, null, frame(pair.toByteArray(), false));

		RayIpc ipc = connect(null, null);
		try {
			ipc.query("(+ 1 \"a\")");
			fail("expected the server's error to be raised");
		} catch (KException e) {
			assertEquals("type", e.getTitle());
			assertTrue(e.getStackMessage(), e.getStackMessage().contains("expects a numeric argument"));
			assertTrue("code is the title, not repeated in the detail",
					!e.getStackMessage().contains("error: type:"));
		}
		ipc.close();
	}

	@Test public void surfacesPrintedOutputAsConsoleText() throws Exception {
		ByteArrayOutputStream pair = new ByteArrayOutputStream();
		pair.write(0);
		pair.write(0);
		pair.write(le64(2));
		pair.write(strAtom("hello from the server\n"));
		pair.write(new byte[] { 126 }); // null result, as (println ...) returns
		server = new FakeServer(false, null, frame(pair.toByteArray(), false));

		RayIpc ipc = connect(null, null);
		RayIpc.Result r = ipc.query("(println \"hello from the server\")");
		ipc.close();
		assertEquals("hello from the server\n", r.getConsole());
		assertEquals(null, r.getValue());
	}

	@Test public void refusesAFrameThatIsNotRayforce() throws Exception {
		byte[] junk = new byte[16 + 4];
		server = new FakeServer(false, null, junk);
		RayIpc ipc = connect(null, null);
		try {
			ipc.query("1");
			fail("expected a framing error");
		} catch (KException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("Not a Rayforce IPC frame"));
		}
		ipc.close();
	}

	// ------------------------------------------------------------- machinery

	private RayIpc connect(String user, String password) throws IOException, KException {
		return new RayIpc("127.0.0.1", server.port(), user, password, 5000);
	}

	/** Serves one connection: handshake, then the same canned reply to every request. */
	private static final class FakeServer implements Runnable {
		private final ServerSocket ss;
		private final boolean wantAuth;
		private final byte versionToReport;
		private final byte[] reply;
		final AtomicReference<String> credentialsSeen = new AtomicReference<>();
		final AtomicReference<byte[]> requestSeen = new AtomicReference<>(new byte[0]);
		volatile boolean rejectAuth = false;

		FakeServer(boolean wantAuth, Byte versionToReport, byte[] reply) throws IOException {
			this.ss = new ServerSocket(0);
			this.wantAuth = wantAuth;
			this.versionToReport = versionToReport == null ? WIRE_VERSION : versionToReport;
			this.reply = reply;
			Thread t = new Thread(this, "fake-rayforce");
			t.setDaemon(true);
			t.start();
		}

		int port() { return ss.getLocalPort(); }

		void close() throws IOException { ss.close(); }

		@Override public void run() {
			try (Socket s = ss.accept()) {
				DataInputStream in = new DataInputStream(s.getInputStream());
				OutputStream out = s.getOutputStream();

				in.readByte(); // client's wire version
				in.readByte(); // reserved
				out.write(new byte[] { versionToReport, (byte) (wantAuth ? 1 : 0) });
				out.flush();

				if (wantAuth) {
					int len = in.readUnsignedByte();
					byte[] cred = new byte[len];
					in.readFully(cred);
					credentialsSeen.set(new String(cred, 0, len - 1, UTF8));
					out.write(rejectAuth ? 1 : 0);
					out.flush();
					if (rejectAuth) {
						return;
					}
				}

				while (true) {
					byte[] header = new byte[16];
					in.readFully(header);
					long size = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getLong(8);
					byte[] body = new byte[(int) size];
					in.readFully(body);

					ByteArrayOutputStream whole = new ByteArrayOutputStream();
					whole.write(header);
					whole.write(body);
					requestSeen.set(whole.toByteArray());

					out.write(reply);
					out.flush();
				}
			} catch (IOException expectedOnClose) {
				// the client hung up, or the test finished
			}
		}
	}

	// ------------------------------------------------- wire-format encoders

	/** Wraps a payload in a response header, compressing it the way the server would. */
	private static byte[] frame(byte[] payload, boolean compressed) throws IOException {
		byte[] body = compressed ? compress(payload) : payload;
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		o.write(le32(PREFIX));
		o.write(WIRE_VERSION);
		o.write(compressed ? 0x01 : 0x00);
		o.write(0); // little endian
		o.write(2); // response
		o.write(le64(body.length));
		o.write(body);
		return o.toByteArray();
	}

	/** Delta code, then run-length encode - ray_ipc_compress in reverse order. */
	private static byte[] compress(byte[] src) throws IOException {
		byte[] delta = new byte[src.length];
		delta[0] = src[0];
		for (int i = 1; i < src.length; i++) {
			delta[i] = (byte) (src[i] - src[i - 1]);
		}

		ByteArrayOutputStream rle = new ByteArrayOutputStream();
		int i = 0;
		while (i < delta.length) {
			int runEnd = i;
			while (runEnd + 1 < delta.length && delta[runEnd + 1] == delta[i] && runEnd - i < 126) {
				runEnd++;
			}
			int runLen = runEnd - i + 1;
			if (runLen > 1) {
				rle.write(runLen);
				rle.write(delta[i]);
				i += runLen;
			} else {
				int litEnd = i;
				while (litEnd + 1 < delta.length && delta[litEnd + 1] != delta[litEnd] && litEnd - i < 126) {
					litEnd++;
				}
				int litLen = litEnd - i + 1;
				rle.write(-litLen);
				rle.write(delta, i, litLen);
				i += litLen;
			}
		}

		ByteArrayOutputStream o = new ByteArrayOutputStream();
		o.write(le32(src.length));
		o.write(rle.toByteArray());
		return o.toByteArray();
	}

	private static byte[] i64Atom(long v) throws IOException {
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		o.write((byte) -5);
		o.write(0); // flags
		o.write(le64(v));
		return o.toByteArray();
	}

	private static byte[] u8Atom(int v, boolean typedNull) throws IOException {
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		o.write((byte) -2);
		o.write(typedNull ? 1 : 0); // flags: low bit marks a typed null
		o.write(v); // the value byte is present either way
		return o.toByteArray();
	}

	private static byte[] list(byte[]... items) throws IOException {
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		o.write(0); // T_LIST
		o.write(0); // attrs
		o.write(le64(items.length));
		for (byte[] item : items) {
			o.write(item);
		}
		return o.toByteArray();
	}

	private static byte[] strAtom(String s) throws IOException {
		byte[] b = s.getBytes(UTF8);
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		o.write((byte) -13);
		o.write(0);
		o.write(le64(b.length));
		o.write(b);
		return o.toByteArray();
	}

	private static byte[] i64Vec(long... vals) throws IOException {
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		o.write(5);
		o.write(0);
		o.write(le64(vals.length));
		for (long v : vals) {
			o.write(le64(v));
		}
		return o.toByteArray();
	}

	private static byte[] symVec(String... vals) throws IOException {
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		o.write(12);
		o.write(0);
		o.write(le64(vals.length));
		for (String v : vals) {
			o.write(v.getBytes(UTF8));
			o.write(0);
		}
		return o.toByteArray();
	}

	private static byte[] errorObject(String code) throws IOException {
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		o.write(127);
		byte[] c = code.getBytes(UTF8);
		o.write(c, 0, Math.min(7, c.length));
		for (int i = c.length; i < 8; i++) {
			o.write(0);
		}
		return o.toByteArray();
	}

	private static byte[] table(String[] colNames, byte[][] cols) throws IOException {
		ByteArrayOutputStream list = new ByteArrayOutputStream();
		list.write(0); // LIST of columns
		list.write(0);
		list.write(le64(cols.length));
		for (byte[] c : cols) {
			list.write(c);
		}

		ByteArrayOutputStream o = new ByteArrayOutputStream();
		o.write(98); // TABLE
		o.write(0); // attrs
		o.write(symVec(colNames)); // schema
		o.write(list.toByteArray());
		return o.toByteArray();
	}

	private static byte[] le32(int v) {
		return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array();
	}

	private static byte[] le64(long v) {
		return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(v).array();
	}
}
