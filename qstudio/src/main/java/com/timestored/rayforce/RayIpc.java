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

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import kx.c;
import kx.c.Dict;
import kx.c.Flip;
import kx.c.KException;

/**
 * Client for the RayforceDB IPC protocol - the equivalent of {@link kx.c} for kdb.
 *
 * Results are decoded into the same Java shapes {@link kx.c} produces for kdb, so
 * everything downstream of a query - the grid, charts, CSV/Excel export, the pivot
 * builder - works on a Rayforce result without knowing where it came from.
 *
 * <h3>Wire format</h3>
 * Handshake is two bytes of version out, two back (second byte set when the server
 * wants credentials), then framed messages: a 16-byte little-endian header followed
 * by a serialized value. See rayforce src/core/ipc.c and src/store/serde.c.
 *
 * The payload is written in the server's native byte order and the header names it,
 * so a big-endian peer is refused rather than silently mis-decoded - the same check
 * rayforce makes.
 */
public class RayIpc {

	/** Wire version we speak. The server refuses a peer that speaks another. */
	private static final byte WIRE_VERSION = 3;
	/** RAY_SERDE_PREFIX - first four bytes of every frame header. */
	private static final int PREFIX = 0xcefadefa;

	private static final byte MSG_SYNC = 1;
	private static final byte FLAG_COMPRESSED = 0x01;
	/**
	 * Ask the server to capture the eval's stdout/stderr and answer with
	 * [captured, result]. Anything the query printed comes back for the console,
	 * and - since an error crosses the wire as a bare 7-byte code - it is also how
	 * the error's detail message reaches us.
	 */
	private static final byte FLAG_VERBOSE = 0x04;

	// Type tags. Negative means atom, positive means vector or compound.
	private static final byte T_LIST = 0;
	private static final byte T_BOOL = 1;
	private static final byte T_U8 = 2;
	private static final byte T_I16 = 3;
	private static final byte T_I32 = 4;
	private static final byte T_I64 = 5;
	private static final byte T_F32 = 6;
	private static final byte T_F64 = 7;
	private static final byte T_DATE = 8;
	private static final byte T_TIME = 9;
	private static final byte T_TIMESTAMP = 10;
	private static final byte T_GUID = 11;
	private static final byte T_SYM = 12;
	private static final byte T_STR = 13;
	private static final byte T_TABLE = 98;
	private static final byte T_DICT = 99;
	private static final byte T_LAMBDA = 100;
	private static final byte T_UNARY = 101;
	private static final byte T_BINARY = 102;
	private static final byte T_VARY = 103;
	private static final byte T_NULL = 126;
	private static final byte T_ERROR = 127;

	/** Rayforce and kdb share an epoch, so these match kx.c's constants. */
	private static final long DAYS_BETWEEN_1970_2000 = 10957L;
	private static final long MILLIS_IN_DAY = 86400000L;
	private static final long MILLIS_BETWEEN_1970_2000 = MILLIS_IN_DAY * DAYS_BETWEEN_1970_2000;
	private static final long NANOS_IN_SEC = 1000000000L;

	private static final short NULL_I16 = Short.MIN_VALUE;
	private static final int NULL_I32 = Integer.MIN_VALUE;
	private static final long NULL_I64 = Long.MIN_VALUE;

	/** Vectors are capped server-side at 1e9 elements; frames are bounded by the heap. */
	private static final int MAX_FRAME = Integer.MAX_VALUE - 8;

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final ZoneId UTC = ZoneId.of("UTC");

	private final Socket socket;
	private final DataInputStream in;
	private final OutputStream out;
	/** Largest reply accepted, in bytes; 0 means no limit. */
	private long maxResultBytes = 0;

	/**
	 * A value a Rayforce eval raised. Only the short code ("type", "domain",
	 * "name", ...) fits on the wire; the formatted detail arrives separately in
	 * the captured output, so {@link #query} pairs the two before throwing.
	 */
	public static final class RayError {
		private final String code;

		RayError(String code) { this.code = code; }

		public String getCode() { return code; }

		@Override public String toString() { return "error: " + code; }
	}

	/** One query's answer: what it printed, and what it evaluated to. */
	public static final class Result {
		private final String console;
		private final Object value;

		Result(String console, Object value) { this.console = console; this.value = value; }

		/** Whatever the query wrote to stdout/stderr on the server. Never null. */
		public String getConsole() { return console; }

		/** The evaluated value in kx.c's Java shapes, or null for a null result. */
		public Object getValue() { return value; }
	}

	/**
	 * Connect and complete the handshake.
	 * @param timeoutMillis bound on the TCP connect; 0 waits for the OS default.
	 */
	public RayIpc(String host, int port, String username, String password, int timeoutMillis)
			throws IOException, KException {
		socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(host, port), timeoutMillis);
			socket.setTcpNoDelay(true);
			in = new DataInputStream(socket.getInputStream());
			out = new BufferedOutputStream(socket.getOutputStream());
			handshake(username, password);
		} catch (IOException e) {
			closeQuietly();
			throw e;
		} catch (KException e) {
			closeQuietly();
			throw e;
		}
	}

	private void handshake(String username, String password) throws IOException, KException {
		out.write(new byte[] { WIRE_VERSION, 0 });
		out.flush();

		byte[] resp = new byte[2];
		in.readFully(resp);
		if (resp[0] != WIRE_VERSION) {
			throw new KException("Rayforce wire version mismatch: server speaks " + resp[0]
					+ ", this client speaks " + WIRE_VERSION);
		}
		if (resp[1] == 0) {
			return; // server is open
		}
		if (resp[1] != 1) {
			throw new KException("Unrecognised Rayforce handshake reply: " + resp[1]);
		}

		// "user:password\0", length-prefixed by a single byte that counts the NUL.
		String user = username == null ? "" : username;
		String pass = password == null ? "" : password;
		byte[] cred = (user + ":" + pass).getBytes(UTF8);
		if (cred.length + 1 > 255) {
			throw new KException("Rayforce credentials too long: " + (cred.length + 1) + " bytes, max 255");
		}
		out.write(cred.length + 1);
		out.write(cred);
		out.write(0);
		out.flush();

		if (in.readByte() != 0) {
			throw new KException("Rayforce authentication failed");
		}
	}

	/** Evaluate {@code expr} on the server and wait for the answer. */
	public Result query(String expr) throws IOException, KException {
		send(expr);
		Object o = receive();

		// Under FLAG_VERBOSE the server answers [captured, result] and guarantees
		// that shape even when its own capture setup failed - but fall back to
		// treating the reply as a bare result if it ever doesn't, rather than
		// mangling a legitimate two-element list.
		String console = "";
		Object value = o;
		if (o instanceof Object[] && ((Object[]) o).length == 2 && ((Object[]) o)[0] instanceof char[]) {
			Object[] pair = (Object[]) o;
			console = new String((char[]) pair[0]);
			value = pair[1];
		}

		if (value instanceof RayError) {
			// The captured text ends with the "error: <code>: <detail>" line the
			// server printed. The code is already the exception's title, so drop
			// that prefix and keep the detail - along with anything the query
			// managed to print before it failed.
			String code = ((RayError) value).getCode();
			throw new KException(code, console.replace("error: " + code + ": ", "").trim());
		}
		return new Result(console, value);
	}

	public boolean isConnected() {
		return socket != null && socket.isConnected() && !socket.isClosed();
	}

	/**
	 * Refuse replies larger than this many bytes - the Rayforce counterpart of
	 * the size check qStudio's kdb query wrapper does server-side. An oversized
	 * frame is drained off the socket so the connection stays usable, but never
	 * decoded, so it cannot exhaust the heap. 0 disables the check.
	 */
	public void setMaxResultBytes(long maxResultBytes) {
		this.maxResultBytes = maxResultBytes;
	}

	public void close() throws IOException {
		socket.close();
	}

	private void closeQuietly() {
		try { socket.close(); } catch (IOException ignored) { }
	}

	// ---------------------------------------------------------------- sending

	private void send(String expr) throws IOException {
		byte[] q = expr.getBytes(UTF8);

		// A query is always a STR atom: type, flags, i64 length, bytes. That is the
		// only shape we ever serialize - the server parses and evaluates the text.
		ByteBuffer frame = ByteBuffer.allocate(16 + 1 + 1 + 8 + q.length).order(ByteOrder.LITTLE_ENDIAN);
		frame.putInt(PREFIX);
		frame.put(WIRE_VERSION);
		frame.put(FLAG_VERBOSE);
		frame.put((byte) 0); // little-endian payload
		frame.put(MSG_SYNC);
		frame.putLong(1 + 1 + 8 + q.length);
		frame.put((byte) -T_STR);
		frame.put((byte) 0); // no typed-null marker
		frame.putLong(q.length);
		frame.put(q);

		out.write(frame.array());
		out.flush();
	}

	// -------------------------------------------------------------- receiving

	private Object receive() throws IOException, KException {
		byte[] header = new byte[16];
		in.readFully(header);
		ByteBuffer h = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);

		if (h.getInt() != PREFIX) {
			throw new KException("Not a Rayforce IPC frame - is this really a Rayforce server?");
		}
		byte version = h.get();
		byte flags = h.get();
		byte endian = h.get();
		h.get(); // msgtype - a sync request only ever gets a response back
		long size = h.getLong();

		if (version != WIRE_VERSION) {
			throw new KException("Rayforce wire version mismatch: server sent " + version
					+ ", this client speaks " + WIRE_VERSION);
		}
		if (endian != 0) {
			throw new KException("Big-endian Rayforce servers are not supported");
		}
		if (size < 0 || size > MAX_FRAME) {
			throw new KException("Rayforce frame size out of range: " + size);
		}
		if (maxResultBytes > 0 && size > maxResultBytes) {
			skipFully(size);
			throw tooLarge(size);
		}

		byte[] payload = new byte[(int) size];
		in.readFully(payload);

		if ((flags & FLAG_COMPRESSED) != 0) {
			if (payload.length < 4) {
				throw new KException("Truncated compressed Rayforce frame");
			}
			int uncompressed = ByteBuffer.wrap(payload, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
			if (uncompressed < 0 || uncompressed > MAX_FRAME) {
				throw new KException("Rayforce uncompressed size out of range: " + uncompressed);
			}
			if (maxResultBytes > 0 && uncompressed > maxResultBytes) {
				throw tooLarge(uncompressed);
			}
			payload = decompress(payload, uncompressed);
		}

		try {
			return decode(ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN));
		} catch (RuntimeException e) {
			// A frame that disagrees with itself - a length that outruns the data -
			// surfaces from ByteBuffer as an unchecked underflow. Report it as the
			// protocol error it is rather than letting it escape as a bug.
			throw new KException("Corrupt Rayforce frame: " + e);
		}
	}

	private KException tooLarge(long bytes) {
		return new KException("Result too large", "The reply is " + (bytes >> 20) + " MB but the query maximum size limit is "
				+ (maxResultBytes >> 20) + " MB. Raise the limit in Settings or return fewer rows, e.g. with a take: clause.");
	}

	private void skipFully(long n) throws IOException {
		while (n > 0) {
			long skipped = in.skip(n);
			if (skipped <= 0) {
				in.readByte(); // skip() may return 0 without EOF; readByte blocks or throws
				skipped = 1;
			}
			n -= skipped;
		}
	}

	/**
	 * Run-length decode then un-delta - the inverse of ray_ipc_compress. The
	 * server compresses any payload over 2000 bytes, so this is on the path of
	 * every result of consequence.
	 */
	static byte[] decompress(byte[] src, int uncompressedLen) throws KException {
		byte[] decoded = new byte[uncompressedLen];
		int si = 4; // skip the uncompressed-length prefix
		int di = 0;

		while (si < src.length && di < uncompressedLen) {
			int count = src[si++]; // signed: positive is a run, negative a literal span
			if (count > 0) {
				if (si >= src.length || di + count > uncompressedLen) {
					throw new KException("Corrupt Rayforce frame: run overruns the buffer");
				}
				byte val = src[si++];
				for (int i = 0; i < count; i++) {
					decoded[di++] = val;
				}
			} else {
				int n = -count;
				if (si + n > src.length || di + n > uncompressedLen) {
					throw new KException("Corrupt Rayforce frame: literal span overruns the buffer");
				}
				System.arraycopy(src, si, decoded, di, n);
				si += n;
				di += n;
			}
		}
		if (di != uncompressedLen) {
			throw new KException("Corrupt Rayforce frame: decompressed " + di + " of " + uncompressedLen + " bytes");
		}

		for (int i = 1; i < di; i++) {
			decoded[i] = (byte) (decoded[i] + decoded[i - 1]);
		}
		return decoded;
	}

	// --------------------------------------------------------------- decoding

	private static Object decode(ByteBuffer b) throws KException {
		byte type = b.get();

		if (type == T_NULL) {
			return null;
		}
		if (type == T_ERROR) {
			byte[] sdata = new byte[8];
			b.get(sdata);
			int len = 0;
			while (len < 7 && sdata[len] != 0) {
				len++;
			}
			return new RayError(new String(sdata, 0, len, UTF8));
		}
		if (type < 0) {
			return decodeAtom(b, (byte) -type);
		}
		return decodeCompound(b, type);
	}

	private static Object decodeAtom(ByteBuffer b, byte base) throws KException {
		// Atoms carry a flags byte whose low bit marks a typed null. The value
		// bytes are written either way, so they are always read and only then
		// discarded - skipping them would desync the rest of the frame.
		boolean isNull = (b.get() & 1) != 0;
		switch (base) {
		case T_BOOL:
			return Boolean.valueOf(b.get() != 0 && !isNull);
		case T_U8: {
			byte v = b.get();
			return Byte.valueOf(isNull ? 0 : v);
		}
		case T_I16: {
			short v = b.getShort();
			return Short.valueOf(isNull ? NULL_I16 : v);
		}
		case T_I32: {
			int v = b.getInt();
			return Integer.valueOf(isNull ? NULL_I32 : v);
		}
		case T_I64: {
			long v = b.getLong();
			return Long.valueOf(isNull ? NULL_I64 : v);
		}
		case T_F32: {
			float v = b.getFloat();
			return Float.valueOf(isNull ? Float.NaN : v);
		}
		case T_F64: {
			double v = b.getDouble();
			return Double.valueOf(isNull ? Double.NaN : v);
		}
		case T_DATE: {
			int v = b.getInt();
			return toDate(isNull ? NULL_I32 : v);
		}
		case T_TIME: {
			int v = b.getInt();
			return toTime(isNull ? NULL_I32 : v);
		}
		case T_TIMESTAMP: {
			long v = b.getLong();
			return toTimestamp(isNull ? NULL_I64 : v);
		}
		case T_GUID:
			return readGuid(b);
		case T_SYM:
			return readCString(b);
		case T_STR:
			return readChars(b);
		default:
			throw new KException("Unknown Rayforce atom type: -" + base);
		}
	}

	private static Object decodeCompound(ByteBuffer b, byte type) throws KException {
		if (type == T_TABLE) {
			b.get(); // attrs - rebuilt from the columns
			Object schema = decode(b);
			Object cols = decode(b);
			if (!(schema instanceof String[]) || !(cols instanceof Object[])) {
				throw new KException("Malformed Rayforce table: expected named columns");
			}
			return new Flip(new Dict(schema, cols));
		}
		if (type == T_DICT) {
			b.get(); // attrs
			Object keys = decode(b);
			Object vals = decode(b);
			return new Dict(keys, vals);
		}
		if (type == T_LAMBDA) {
			b.get(); // attrs
			decode(b); // params
			decode(b); // body
			// kdb functions arrive as char[] and qStudio renders them as source
			// rather than as a table. Rayforce sends a compiled tree, not text.
			return "(fn ...)".toCharArray();
		}
		if (type == T_UNARY || type == T_BINARY || type == T_VARY) {
			return readCString(b).toCharArray();
		}

		b.get(); // attrs - nulls are sentinel-encoded in the payload, no bitmap
		int n = readLength(b);

		switch (type) {
		case T_LIST: {
			Object[] r = new Object[n];
			for (int i = 0; i < n; i++) {
				r[i] = decode(b);
			}
			return r;
		}
		case T_BOOL: {
			boolean[] r = new boolean[n];
			for (int i = 0; i < n; i++) {
				r[i] = b.get() != 0;
			}
			return r;
		}
		case T_U8: {
			byte[] r = new byte[n];
			b.get(r);
			return r;
		}
		case T_I16: {
			short[] r = new short[n];
			for (int i = 0; i < n; i++) {
				r[i] = b.getShort();
			}
			return r;
		}
		case T_I32: {
			int[] r = new int[n];
			for (int i = 0; i < n; i++) {
				r[i] = b.getInt();
			}
			return r;
		}
		case T_I64: {
			long[] r = new long[n];
			for (int i = 0; i < n; i++) {
				r[i] = b.getLong();
			}
			return r;
		}
		case T_F32: {
			float[] r = new float[n];
			for (int i = 0; i < n; i++) {
				r[i] = b.getFloat();
			}
			return r;
		}
		case T_F64: {
			double[] r = new double[n];
			for (int i = 0; i < n; i++) {
				r[i] = b.getDouble();
			}
			return r;
		}
		case T_DATE: {
			LocalDate[] r = new LocalDate[n];
			for (int i = 0; i < n; i++) {
				r[i] = toDate(b.getInt());
			}
			return r;
		}
		case T_TIME: {
			LocalTime[] r = new LocalTime[n];
			for (int i = 0; i < n; i++) {
				r[i] = toTime(b.getInt());
			}
			return r;
		}
		case T_TIMESTAMP: {
			Instant[] r = new Instant[n];
			for (int i = 0; i < n; i++) {
				r[i] = toTimestamp(b.getLong());
			}
			return r;
		}
		case T_GUID: {
			UUID[] r = new UUID[n];
			for (int i = 0; i < n; i++) {
				r[i] = readGuid(b);
			}
			return r;
		}
		case T_SYM: {
			String[] r = new String[n];
			for (int i = 0; i < n; i++) {
				r[i] = readCString(b);
			}
			return r;
		}
		case T_STR: {
			// A vector of strings, not kdb's list-of-char-lists: String[] is what
			// the grid wants and what a symbol column already looks like.
			String[] r = new String[n];
			for (int i = 0; i < n; i++) {
				r[i] = new String(readChars(b));
			}
			return r;
		}
		default:
			throw new KException("Unknown Rayforce type: " + type);
		}
	}

	private static int readLength(ByteBuffer b) throws KException {
		long n = b.getLong();
		if (n < 0 || n > b.remaining()) {
			// Every element occupies at least one byte, so a count above the bytes
			// left cannot be real - reject before allocating for it.
			throw new KException("Rayforce vector length out of range: " + n);
		}
		return (int) n;
	}

	private static String readCString(ByteBuffer b) throws KException {
		int start = b.position();
		while (b.hasRemaining()) {
			if (b.get() == 0) {
				int len = b.position() - start - 1;
				byte[] s = new byte[len];
				b.position(start);
				b.get(s);
				b.get(); // step over the terminator
				return new String(s, UTF8);
			}
		}
		throw new KException("Corrupt Rayforce frame: symbol with no terminator");
	}

	private static char[] readChars(ByteBuffer b) throws KException {
		int n = readLength(b);
		byte[] s = new byte[n];
		b.get(s);
		return new String(s, UTF8).toCharArray();
	}

	private static UUID readGuid(ByteBuffer b) {
		long hi = b.order(ByteOrder.BIG_ENDIAN).getLong();
		long lo = b.getLong();
		b.order(ByteOrder.LITTLE_ENDIAN);
		return new UUID(hi, lo);
	}

	private static LocalDate toDate(int daysSince2000) {
		return daysSince2000 == NULL_I32 ? LocalDate.MIN
				: LocalDate.ofEpochDay(DAYS_BETWEEN_1970_2000 + daysSince2000);
	}

	private static LocalTime toTime(int millisSinceMidnight) {
		return millisSinceMidnight == NULL_I32 ? c.LOCAL_TIME_NULL
				: LocalDateTime.ofInstant(Instant.ofEpochMilli(millisSinceMidnight), UTC).toLocalTime();
	}

	private static Instant toTimestamp(long nanosSince2000) {
		if (nanosSince2000 == NULL_I64) {
			return Instant.MIN;
		}
		long secs = nanosSince2000 < 0 ? (nanosSince2000 + 1) / NANOS_IN_SEC - 1 : nanosSince2000 / NANOS_IN_SEC;
		return Instant.ofEpochMilli(MILLIS_BETWEEN_1970_2000 + 1000 * secs)
				.plusNanos(nanosSince2000 - NANOS_IN_SEC * secs);
	}
}
