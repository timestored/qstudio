/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2026 TimeStored
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 */
package com.timestored.connections;

import java.util.List;
import java.util.Optional;

import com.timestored.connections.ConnectionShortFormat.ParseResult;

/**
 * Parses a single line of user-pasted text (clipboard or the Quick Connect
 * prompt) into a {@link ServerConfig}. Input that does not match the strict
 * whitelist returns {@link Optional#empty()}. kdb and short-format variants
 * delegate to {@link ConnectionShortFormat} so the accepted syntax is
 * identical to the existing "Add Server List" bulk-add dialog.
 */
public final class HandleParser {

	/** Inputs longer than this are rejected defensively. */
	static final int MAX_LEN = 256;

	private HandleParser() {}

	/**
	 * @param raw               raw clipboard / prompt text; may be {@code null}
	 * @param hasKdbConnections whether the user has any kdb connection configured — controls
	 *                          whether a bare {@code host:port} (no scheme, no {@code TYPE#}
	 *                          marker) is accepted as kdb
	 * @return parsed config, or {@link Optional#empty()} if input does not match the whitelist
	 */
	public static Optional<ServerConfig> parse(String raw, boolean hasKdbConnections) {
		String s = preprocess(raw);
		if (s == null) return Optional.empty();

		String lower = s.toLowerCase();

		if (lower.startsWith("http://")  || lower.startsWith("https://")
		 || lower.startsWith("ssh://")   || lower.startsWith("ftp://")) {
			return Optional.empty();
		}
		if (lower.startsWith("jdbc:"))         return parseJdbc(s);
		if (lower.startsWith("postgresql://")) return parseNativeUri(s, JdbcTypes.POSTGRES);
		if (lower.startsWith("mysql://"))      return parseNativeUri(s, JdbcTypes.MYSQL);
		if (lower.startsWith("duckdb:"))       return parseDuckdbFile(s.substring("duckdb:".length()));

		return parseShortFormat(s, hasKdbConnections);
	}

	/** Returns the normalised input, or {@code null} if empty / over-length / blank / binary. */
	private static String preprocess(String raw) {
		if (raw == null) return null;
		String s = raw.replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ');
		s = s.trim();
		if (s.isEmpty() || s.length() > MAX_LEN) return null;
		if (s.length() >= 2) {
			char f = s.charAt(0);
			char l = s.charAt(s.length() - 1);
			if ((f == '"' && l == '"') || (f == '\'' && l == '\'') || (f == '`' && l == '`')) {
				s = s.substring(1, s.length() - 1).trim();
				if (s.isEmpty()) return null;
			}
		}
		if (s.startsWith("`")) s = s.substring(1);
		if (s.startsWith(":")) s = s.substring(1);
		s = s.trim();
		if (s.isEmpty()) return null;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) < 0x20) return null;
		}
		return s;
	}

	private static Optional<ServerConfig> parseShortFormat(String s, boolean hasKdbConnections) {
		if (!s.contains(":")) return Optional.empty();
		boolean hasExplicitType = s.contains("#") || s.contains("@");
		if (!hasExplicitType && !hasKdbConnections) return Optional.empty();
		List<ParseResult> results = ConnectionShortFormat.parse(s, JdbcTypes.KDB, JdbcTypes.values());
		if (results.isEmpty() || results.get(0).serverConfig == null) return Optional.empty();
		ServerConfig sc = results.get(0).serverConfig;
		if (sc.getHost() == null || sc.getHost().isEmpty()) return Optional.empty();
		return Optional.of(sc);
	}

	private static Optional<ServerConfig> parseJdbc(String s) {
		String body = s.substring("jdbc:".length());
		int c = body.indexOf(':');
		if (c < 0) return Optional.empty();
		String driver = body.substring(0, c).toLowerCase();
		String rest = body.substring(c + 1);
		switch (driver) {
			case "postgresql": return parseHostPortUri(rest, JdbcTypes.POSTGRES);
			case "mysql":      return parseHostPortUri(rest, JdbcTypes.MYSQL);
			case "duckdb":     return parseDuckdbFile(rest);
			default:           return Optional.empty();
		}
	}

	private static Optional<ServerConfig> parseNativeUri(String s, JdbcTypes type) {
		int c = s.indexOf(':');
		return parseHostPortUri(s.substring(c + 1), type);
	}

	private static Optional<ServerConfig> parseHostPortUri(String rest, JdbcTypes type) {
		if (!rest.startsWith("//")) return Optional.empty();
		String body = rest.substring(2);
		String user = "", pass = "";
		int at = body.indexOf('@');
		if (at >= 0) {
			String userinfo = body.substring(0, at);
			body = body.substring(at + 1);
			int pc = userinfo.indexOf(':');
			if (pc >= 0) {
				user = userinfo.substring(0, pc);
				pass = userinfo.substring(pc + 1);
			} else {
				user = userinfo;
			}
		}
		String hostPort = body;
		String db = "";
		int slash = body.indexOf('/');
		if (slash >= 0) {
			hostPort = body.substring(0, slash);
			db = body.substring(slash + 1);
		}
		String host;
		int port = type.getDefaultPort();
		int pc = hostPort.indexOf(':');
		if (pc >= 0) {
			host = hostPort.substring(0, pc);
			try {
				port = Integer.parseInt(hostPort.substring(pc + 1));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		} else {
			host = hostPort;
		}
		if (host.isEmpty()) return Optional.empty();
		String name = host + ":" + port;
		return Optional.of(new ServerConfig(host, port, user, pass, name, type, null, db));
	}

	/** DuckDB URLs carry a file path, not host:port. Port 0 → database field holds the full JDBC URL. */
	private static Optional<ServerConfig> parseDuckdbFile(String path) {
		if (path.isEmpty()) return Optional.empty();
		String url = "jdbc:duckdb:" + path;
		String name = "duckdb:" + path;
		return Optional.of(new ServerConfig("localhost", 0, "", "", name, JdbcTypes.DUCKDB, null, url));
	}
}
