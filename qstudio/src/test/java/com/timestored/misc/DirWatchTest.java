package com.timestored.misc;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileFilter;

import org.junit.Test;

import com.google.common.io.Files;
import com.timestored.misc.DirWatch.DirWatchListener;

public class DirWatchTest {

	@Test public void testListeningToChange() throws Exception {

		// Create and add listener
		DirWatch dw = new DirWatch(1000, pathname  -> true, false);
		TestDirWatchListener testDirWatchListener = new TestDirWatchListener();
		dw.addListener(testDirWatchListener);
		
		// setRoot then change
		File temp1 = Files.createTempDir();
		dw.setRoot(temp1);
		File temp2 = Files.createTempDir();
		dw.setRoot(temp2);
		
		// check changes to first folder has no effect
		assertFalse(testDirWatchListener.changeOccurred);
		new File(temp1, "test1").createNewFile();
		Thread.sleep(2000);
		assertFalse(testDirWatchListener.changeOccurred);
		
		// changing latest root, should cause notification
		new File(temp2, "test2").createNewFile();
		Thread.sleep(2000);
		assertTrue(testDirWatchListener.changeOccurred);
	}

	
	private static class TestDirWatchListener implements DirWatchListener {
		
		boolean changeOccurred = false;
		
		@Override public void changeOccurred() {
			changeOccurred = true;
		}
	}
}
