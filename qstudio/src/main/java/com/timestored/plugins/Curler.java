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
/********************************************************************************************************************
 * 
 * COPIED FROM PULSE - Keep full copy there
 *
 *******************************************************************************************************************/
/********************************************************************************************************************
 * 
 * COPIED FROM PULSE - Keep full copy there
 *
 *******************************************************************************************************************/



package com.timestored.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

import lombok.Data;
import lombok.extern.java.Log;

@Log
public class Curler {

    @Data
    private static class URLdetails {
    	
    	public URLdetails(String fullURL) {
    		String auth  = "";
    		String base64  = null;
    		String u = Objects.requireNonNull(fullURL);
    		if(u.contains("@") && u.contains("//")) {
    			auth  = fullURL.substring(u.indexOf("//")+2, u.indexOf('@'));
    	        u = fullURL.substring(0, fullURL.indexOf("//")+2) + fullURL.substring(fullURL.indexOf('@')+1);
				try {
					byte[] data1 = auth.getBytes(StandardCharsets.UTF_8.name());
	    	        base64 = Base64.getEncoder().encodeToString(data1);
				} catch (UnsupportedEncodingException e) {
					log.severe("URLdetails UnsupportedEncodingException1");
				}
    		}
    		this.u = u;
    		this.auth = auth;
    		this.base64 = base64;
		}
    	
    	private final String u;
    	private final String auth;
    	private final String base64;
    }
    
    private static boolean curlExists = false;
    private static boolean curlTested = false;
	private static final String V2 = "https://www.timestored.com/qstudio/version2.txt";
    
    private static boolean doesCurlExist() {
    	if(curlTested) {
    		return curlExists;
    	}
    	curlTested = true;
    	try {
    		String s = curlFetchURL(V2);
    		curlExists = s!=null && s.length() > 0;
    		if(!curlExists) {
        		log.warning("curl does NOT exist. Pinging testurl failed");
    		}
    	} catch(IOException e) {
    		log.warning("curl failed: " + e.getLocalizedMessage());
    	}
    	return curlExists;
    }
    
	public static String fetchURL(String url) {
		// Some places use https with custom certs. Those certs may not be installed in the java distribution running pulse.
		// Therefore fall back is to call curl and hope the system has certs and/or proxy setup.
		String r = null;
		try {
			r = javaFetchURL(url);
		} catch (Exception e) {
			// hiding url as it would contain password
			log.warning("javaFetchURL failed: " + e.getLocalizedMessage().replace(url, "URLURL"));
		}
		if(r == null && doesCurlExist()) {
			try {
				r = curlFetchURL(url);
			} catch (IOException e) {
				log.warning("curlFetchURL failed: " + e.getLocalizedMessage().replace(url, "URLURL"));
			}
		}
		return r;
    }

	private static String getUrlArgForCurl(URLdetails ud) {
		boolean isWin = System.getProperty("os.name").toLowerCase().contains("win"); 
		return isWin ? "\""+ud.u.replace("\"", "\\\"") +"\"" : ud.u;
	}
	
	static String curlFetchURL(String url) throws IOException {
		URLdetails ud = new URLdetails(Objects.requireNonNull(url));
		String auth = ud.getAuth() == null || ud.getAuth().length() < 1 ? "a:b" : ud.getAuth(); 
		String[] args =  new String[] {"curl", "--globoff", "--silent", "-u", auth, getUrlArgForCurl(ud)};
		return runArgs(args);
	}

	private static String runArgs(String[] args) throws IOException {
		Process process = new ProcessBuilder(args).redirectErrorStream(true).start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		BufferedReader bre = new BufferedReader (new InputStreamReader(process.getErrorStream()));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ( (line = reader.readLine()) != null) {
		   sb.append(line);
		   sb.append(System.getProperty("line.separator"));
		}
		reader.close();
		while ((line = bre.readLine()) != null) { 
			System.err.println(line); 
		}
	    bre.close();
		
//		try {
//			int exitVal = process.waitFor();
//			System.out.println("exitVal = " + exitVal);
//		} catch (InterruptedException e) {}
		return sb.toString();
	}

	static String javaFetchURL(String url) throws IOException {
		URLdetails ud = new URLdetails(Objects.requireNonNull(url));
    	HttpURLConnection urlConnection;
        urlConnection = (HttpURLConnection) ((new URL(ud.u).openConnection()));
        if(ud.base64 != null) {
	        urlConnection.setRequestProperty("Authorization", "Basic "+ud.base64);	
        }
        urlConnection.setRequestProperty("Accept", "text/plain"); // application/json
        urlConnection.setRequestMethod("GET");
		if(urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) { // success
			StringBuffer response = new StringBuffer();
			try(BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine).append("\n");
				}
			}
			return response.toString();
		} else {
			log.warning("HTTP request failed");
		}
		return null;
    }

	
	public static boolean downloadFileTo(String url, File f) throws IOException {
		if(!f.getParentFile().isDirectory() && !f.getParentFile().mkdirs()) {
			throw new IOException(f.getAbsolutePath() + " not creatable");
		}
		log.info("Downloading " + url + " -> " + f.getAbsolutePath());
		// Some places use https with custom certs. Those certs may not be installed in the java distribution running pulse.
		// Therefore fall back is to call curl and hope the system has certs and/or proxy setup.
		boolean r = false;
		try {
			javaDownloadFileTo(url, f);
			r = true;
		} catch (Exception e) {
			// hiding url as it would contain password
			log.warning("javaDownloadFileTo failed: " + e.getLocalizedMessage().replace(url, "URLURL"));
		}
		if(r == false && doesCurlExist()) {
			try {
				curlDownloadFileTo(url, f);
				r = true;
			} catch (Exception e) {
				log.warning("curlDownloadFileTo failed: " + e.getLocalizedMessage().replace(url, "URLURL"));
			}
		}
		return r;
	}

	static void curlDownloadFileTo(String url, File f) throws IOException {
		URLdetails ud = new URLdetails(Objects.requireNonNull(url));
//		String auth = ud.getAuth() == null || ud.getAuth().length() < 1 ? "a:b" : ud.getAuth(); 
		// -L is follow redirects, --fail causes 404 etc. to throw errors
		String[] args =  new String[] {"curl", "--globoff", "-L", "--fail", "-o", f.getAbsolutePath(), getUrlArgForCurl(ud)};
		runArgs(args);
	}

	static void javaDownloadFileTo(String url, File f) throws FileNotFoundException, IOException {
		try (InputStream in = new URL(url).openStream();
			ReadableByteChannel rbc = Channels.newChannel(in);
			FileOutputStream fos = new FileOutputStream(f)) {
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		}
	}
}
