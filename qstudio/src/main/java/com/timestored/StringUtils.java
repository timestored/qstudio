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

package com.timestored;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Common String operations 
 */
public class StringUtils {

	/**
	 * A null safe equality check for two strings
	 * @return true if the strings are equal, including possibly both null.
	 */
	public static boolean equals(String a, String b) {
		if(a == null) {
			return b == null;
		}
		return a.equals(b);
	}
	
	/**
	 * Abbreviates a String using ellipses.
	 * @param str the String to check, may be null
	 * @param maxWidth maximum length of result String, must be at least 4
	 * @return abbreviated String, {@code null} if null String input
	 * @throws IllegalArgumentException if the width is too small
	 */
	public static String abbreviate(final String str, final int maxWidth) {
		return abbreviate(str, 0, maxWidth);
	}

	public static String abbreviateMultiline(final String str) {
		return abbreviateMultiline(str, 7, 60);
	}

	public static String abbreviateMultiline(final String str, final int maxRows, final int maxWidth) {
		String[] r = str.split("\\n", maxRows+4);
		StringBuilder sb = new StringBuilder(maxRows * maxWidth * 2);
		for(int i=0; i<maxRows && i<r.length; i++) {
			String s = r[i];
			sb.append(abbreviate(s, 0, maxWidth)).append("\n");
		}
		if(r.length > maxRows) {
			sb.append("...");
		}
		return sb.toString().trim();
	}
	
	/**
	 * Abbreviates a String using ellipses.
	 * @param str the String to check, may be null
	 * @param offset left edge of source String
	 * @param maxWidth maximum length of result String, must be at least 4
	 * @return abbreviated String, {@code null} if null String input
	 * @throws IllegalArgumentException if the width is too small
	 */
	public static String abbreviate(final String str, int offset,
			final int maxWidth) {
		if (str == null) {
			return null;
		}
		if (maxWidth < 4) {
			throw new IllegalArgumentException("Minimum abbreviation width is 4");
		}
		if (str.length() <= maxWidth) {
			return str;
		}
		if (offset > str.length()) {
			offset = str.length();
		}
		if (str.length() - offset < maxWidth - 3) {
			offset = str.length() - (maxWidth - 3);
		}
		final String abrevMarker = "...";
		if (offset <= 4) {
			return str.substring(0, maxWidth - 3) + abrevMarker;
		}
		if (maxWidth < 7) {
			throw new IllegalArgumentException(
					"Minimum abbreviation width with offset is 7");
		}
		if (offset + maxWidth - 3 < str.length()) {
			return abrevMarker
					+ abbreviate(str.substring(offset), maxWidth - 3);
		}
		return abrevMarker + str.substring(str.length() - (maxWidth - 3));
	}
	
	 public static byte[] compress(String text) {
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        try {
	            OutputStream out = new DeflaterOutputStream(baos);
	            out.write(text.getBytes("UTF-8"));
	            out.close();
	        } catch (IOException e) {
	            throw new AssertionError(e);
	        }
	        return baos.toByteArray();
	    }

	    public static String decompress(byte[] bytes) {
	        InputStream in = new InflaterInputStream(new ByteArrayInputStream(bytes));
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        try {
	            byte[] buffer = new byte[8192];
	            int len;
	            while((len = in.read(buffer))>0)
	                baos.write(buffer, 0, len);
	            return new String(baos.toByteArray(), "UTF-8");
	        } catch (IOException e) {
	            throw new AssertionError(e);
	        }
	    }
}
