package com.timestored.misc;

import java.io.IOException;

import static org.junit.Assert.*;
import org.junit.Test;

public class CmdRunnerTest {

	@Test public void testStartProc() throws InterruptedException, IOException {
		Process ps = CmdRunner.startProc(new String[] { "cmd.exe", "/c", "echo", "a" },  null, null);
		ps.waitFor();
	}

	@Test public void testRun() throws InterruptedException, IOException {
		String s = CmdRunner.run(new String[] { "cmd.exe", "/c", "echo", "a" });
		assertEquals("a\r\n", s);
	}
}
