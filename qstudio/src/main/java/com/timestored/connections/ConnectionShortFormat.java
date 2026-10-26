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
package com.timestored.connections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

/**
 * Accepts multi-line strings and converts to {@link ServerConfig}s.<br>
 *<br>
 * The format is:<br>
 * 		longName@MYSQL#localhost:5000:username:password<br>
 * Or since longName / jdbcType / username / password are optional, can be<br>
 * 		localhost:5000
 */
public class ConnectionShortFormat {

	public static class ParseResult {
		
		/** Will hold {@link ServerConfig} is parsing was successful, otherwise null. */
		public final ServerConfig serverConfig;

		/**  "" means that ServerConfig was created ok, otherwise gives error details. */
		public final String report;
		public final String originalLine;
		
		public ParseResult(String originalLine, ServerConfig serverConfig) {
			this(originalLine, serverConfig, "");
		}
		
		public ParseResult(String originalLine, String error) {
			this(originalLine, null, error);
		}

		private ParseResult(String originalLine, ServerConfig serverConfig, String error) {
			this.serverConfig = serverConfig;
			this.report = error;
			this.originalLine = Preconditions.checkNotNull(originalLine);
		}
	}

	/**
	 * Parse the supplied string into an array of results ({@link ServerConfig} where possible),
	 * invalid strings will not throw exceptions but will have null {@link ServerConfig}.
	 * @param serverEntries A String of the full form:<br>
	 * 		longName@MYSQL#localhost:5000:username:password<br>
	 * 		See class description for full details.
	 * @param defaultServerType The Jdbc type assumed if not specified.
	 * @param permittedJdbcTypes The permitted Jdbc Types, must include atleast default.
	 * @return Array of results with one entry for each line of string argument
	 */
	public static List<ParseResult> parse(String serverEntries, JdbcTypes defaultServerType, 
			JdbcTypes[] permittedJdbcTypes) {

		Preconditions.checkNotNull(defaultServerType);
		Preconditions.checkNotNull(permittedJdbcTypes);
		Set<JdbcTypes> permittedTypes = new HashSet(Arrays.asList(permittedJdbcTypes));
		Preconditions.checkArgument(permittedTypes.contains(defaultServerType));
		
		String[] serverLines = serverEntries.split("\n");
		List<ParseResult> r = new ArrayList<ParseResult>(serverLines.length);
		
		for(int i=0; i<serverLines.length; i++) {
			
			String l = serverLines[i].trim();
			if(l.length() > 0) {
				r.add(parseLine(defaultServerType, permittedTypes, l));
			} 
		}
		
		return r;
	}

	private static ParseResult parseLine(JdbcTypes defaultServerType, Set<JdbcTypes> permittedTypes, 
			String line) {
		
		ParseResult pr;
		String l = line;
		String name = "";
		try {
			int atPos = l.indexOf("@");
			if(atPos > -1) {
				name = l.substring(0, atPos).trim();
				l = l.substring(atPos+1).trim();
			}
			
			String[] p = l.split(":");
			
			if(p.length>=2) {
				
				String host = p[0];
				int port = Integer.parseInt(p[1]);
				

				// careful when no name specified that we take folder off the host
				int lastSlash = host.lastIndexOf("/");
				if(lastSlash > -1) {
					name = host + ":" + port;
					host = host.substring(lastSlash + 1);
				}
				
				String username = "";
				String password = "";
				JdbcTypes jtype = defaultServerType;

				
				int hashPos = host.indexOf("#");
				if(hashPos > -1) {
					jtype = JdbcTypes.valueOf(host.substring(0, hashPos).trim());
					if(!permittedTypes.contains(jtype)) {
						throw new IllegalArgumentException("Illegal JDBC connection type");
					}
					host = host.substring(hashPos+1).trim();
				}
				
				if(p.length>=3) {
					username = p[2].trim();
				}
				if(p.length>=4) {
					password = p[3].trim();
				}

				pr = new ParseResult(l, new ServerConfig(host, port, username, 
						password, name, jtype));
				
			} else {
				pr = new ParseResult(l, "No : Found so could not parse hostname:port");
			}
		} catch(NumberFormatException nfe) {
			pr = new ParseResult(l, "Error parsing number in definition");
		} catch(IllegalArgumentException iae) {
			pr = new ParseResult(l, "Error parsing: " + iae.getMessage());
		}
		return pr;
	}

	public static String compose(List<ServerConfig> serverConfs, JdbcTypes defaultServerType) {
	    return serverConfs.stream()
	            .map(sc -> toString(sc, defaultServerType))
	            .collect(Collectors.joining("\r\n"));
	}

	public static String toString(ServerConfig sc) {
		return toString(sc, JdbcTypes.KDB);
	}
	
	public static String toString(ServerConfig sc, JdbcTypes defaultServerType) {
	    StringBuilder sb = new StringBuilder();

	    String hp = sc.getHost() + ":" + sc.getPort();

	    // optional name/jdbcType
	    if (!sc.getName().equals(hp)) {
	        sb.append(sc.getName()).append("@");
	    }
	    if (!defaultServerType.equals(sc.getJdbcType())) {
	        sb.append(sc.getJdbcType().name()).append("#");
	    }

	    sb.append(hp);

	    // optional username / password
	    if (!sc.getUsername().isEmpty() || !sc.getPassword().isEmpty()) {
	        sb.append(":").append(sc.getUsername());
	    }
	    if (!sc.getPassword().isEmpty()) {
	        sb.append(":").append(sc.getPassword());
	    }

	    return sb.toString();
	}
}
