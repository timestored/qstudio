/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2024 TimeStored
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
package com.timestored.misc;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Component;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.Files;


public class TestHelper {

	private static final int DEFAULT_WIDTH = 400;
	private static final int DEFAULT_HEIGHT = 400;
	
	private static final String TEST_OUTPUT_DIR = "target" + File.separator 
			+ "testout" + File.separator;
	   

	
	public static String getOutDir() throws IOException {
		System.out.println(new File(".").getAbsolutePath());
		
		Files.createParentDirs(new File(TEST_OUTPUT_DIR + File.separator + "a.txt"));
		return TEST_OUTPUT_DIR;
	}

	public static File saveComponentImage(Component c, final String filename, int width, int height) throws IOException {
		File f = new File(getOutDir() + filename);
		SaveableFrame.saveComponentImage(c, width, height, f);
		return f;
		
	}

	public static File saveComponentImage(Component c, final String filename) throws IOException {
		File f = new File(getOutDir() + filename);
		SaveableFrame.saveComponentImage(c, DEFAULT_WIDTH, DEFAULT_HEIGHT, f);
		return f;
	}


	public static boolean assertFilesMatch(File file, InputStream knownFile) {
		if(knownFile != null) {
			try {
				String msg = "checking file match for: " + file.getName();
				assertTrue(msg, isEqual(new FileInputStream(file), knownFile));
				return true;
			} catch (FileNotFoundException e) {
				fail("generated image not found: " + e.toString());
			} catch (IOException e) {
				fail("generated image IO fail: " + e.toString());
			}
		} else {
			fail("known image not found: " +  file.getName());
		}
		return false;
	}
	

	/** @return true only if two input streams are equal. */
	private static boolean isEqual(InputStream i1, InputStream i2) throws IOException {
	    byte[] buf1 = new byte[64 *1024];
	    byte[] buf2 = new byte[64 *1024];
	    try {
	        DataInputStream d2 = new DataInputStream(i2);
	        int len;
	        while ((len = i1.read(buf1)) > 0) {
	            d2.readFully(buf2,0,len);
	            for(int i=0;i<len;i++)
	              if(buf1[i] != buf2[i]) return false;
	        }
	        return d2.read() < 0; // is the end of the second file also.
	    } catch(EOFException ioe) {
	        return false;
	    } finally {
	        i1.close();
	        i2.close();
	    }
	}
	


}
