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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.timestored.connections.JdbcTypes;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public class AIFacade {

	@Getter @Setter private static String openAIkey = "";

	private static final String SQL_PREP = "You are an sql expert. Given an input question, step by step create a syntactically correct sql query to run.\r\n"
			+ "Unless the user specifies in the question a specific number of examples to obtain, query for at most 1000 results using the LIMIT clause as per SQL. You can order the results to return the most informative data in the database.\r\n"
			+ "Never query for all columns from a table. You must query only the columns that are needed to answer the question. Wrap each column name in double quotes (\") to denote them as delimited identifiers.\r\n"
			+ "Pay attention to use only the column names you can see in the tables below. Be careful to not query for columns that do not exist. Also, pay attention to which column is in which table.\r\n"
			+ "";
	private static final String SQL_Q1 = "Question: Select the first two rows from the trade table?";
	private static final String SQL_A1 = "Answer: SELECT * FROM trade LIMIT 2";
	private static final String SQL_Q2 = "Question: Select the most recent 20 minutes of 'NFLX' bid ask quotes?";
	private static final String SQL_A2 = "Answer: SELECT TIME,BID,ASK FROM quote WHERE NAME='NFLX' AND TIME>timestampadd(minute,-20,date_trunc('minute',CURRENT_TIMESTAMP())) ORDER BY TIME ASC;";
	private static final String SQL_Q3 = "Question: Find the number of trades for JPM grouped by week?";
	private static final String SQL_A3 = "Answer: select COUNT(*) as trade_count,DATE_TRUNC('week', time)  as ttime FROM trade WHERE symbol = 'JPM' GROUP BY ttime";

	private static final String KDB_PREP = "You are a kdb+ expert. Given an input question, step by step create a syntactically correct kdb query to run.\r\n"
			+ "Unless the user specifies in the question a specific number of examples to obtain, query for at most 5 results using take #. \r\n"
			+ "Pay attention to use only the column names you can see in the tables below. \r\n"
			+ "Be careful to not query for columns that do not exist. Also, pay attention to which column is in which table.\r\n"
			+ "'ORDER BY' does not work in kdb. LIMIT does not work in kdb. 'GROUP BY' does not work in kdb.\r\n"
			+ "\r\n" + "Only use the following tables:";

	private static final String KDB_Q1 = "Question: Select the first two rows from the trade table?";
	private static final String KDB_A1 = "Answer: select time,sym,status,quantity,destination,orderType,percent,pnl,price,name,avgPrice from trade where date=.z.d-1,i<2";
	private static final String KDB_Q2 = "Question: Find the number of trades for JPM grouped by week?";
	private static final String KDB_A2 = "Answer: select count i by 7 xbar `date$time from trade where sym=`JPM";
	private static final String KDB_Q3 = "Question: Find the price of 'NFLX' trades in 15 minute bars from trades?";
	private static final String KDB_A3 = "Answer: select count i by 15 xbar time.minute from trade where sym=`NFLX";

	private static final String getKDBMessages(String tblInfo, String question) {
		String tbls = tblInfo != null ? tblInfo : "";
		return "[{\"role\": \"user\", \"content\": \"" + KDB_PREP.replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\"")
				+ tbls.replace("\n", "\\n").replace("\r", "\\r") + "\"}\r\n"
				+ ",{\"role\": \"user\", \"content\": \"" + KDB_Q1 + "\"}\r\n"
				+ ",{\"role\": \"assistant\", \"content\": \"" + KDB_A1 + "\"}\r\n"
				+ ",{\"role\": \"user\", \"content\": \"" + KDB_Q2 + "\"}\r\n"
				+ ",{\"role\": \"assistant\", \"content\": \"" + KDB_A2 + "\"}\r\n"
				+ ",{\"role\": \"user\", \"content\": \"" + KDB_Q3 + "\"}\r\n"
				+ ",{\"role\": \"assistant\", \"content\": \"" + KDB_A3 + "\"}\r\n"
				+ ",{\"role\": \"user\", \"content\": \"Question: "
				+ question.replace("\n", "\\n").replace("\r", "\\r") + "\"}\r\n"
				+ ",{\"role\": \"assistant\", \"content\": \"Answer: \"}\r\n" + "]\r\n";
	}

	private static final String getSQLMessages(String tblInfo, String question) {
		String tbls = tblInfo != null ? tblInfo : "";
		return "[{\"role\": \"user\", \"content\": \"" + SQL_PREP.replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\"")
				+ tbls.replace("\n", "\\n").replace("\r", "\\r") + "\"}\r\n"
				+ ",{\"role\": \"user\", \"content\": \"" + SQL_Q1 + "\"}\r\n"
				+ ",{\"role\": \"assistant\", \"content\": \"" + SQL_A1 + "\"}\r\n"
				+ ",{\"role\": \"user\", \"content\": \"" + SQL_Q2 + "\"}\r\n"
				+ ",{\"role\": \"assistant\", \"content\": \"" + SQL_A2 + "\"}\r\n"
				+ ",{\"role\": \"user\", \"content\": \"" + SQL_Q3 + "\"}\r\n"
				+ ",{\"role\": \"assistant\", \"content\": \"" + SQL_A3 + "\"}\r\n"
				+ ",{\"role\": \"user\", \"content\": \"Question: "
				+ question.replace("\n", "\\n").replace("\r", "\\r") + "\"}\r\n"
				+ ",{\"role\": \"assistant\", \"content\": \"Answer: \"}\r\n" + "]\r\n";
	}

	
	@Data
	public static class AIresult {
		private final String jsonReturned;
		private final String firstContent;
		private final String firstCode;
	}
	
	public static AIresult queryOpenAIstructured(JdbcTypes jdbcType, String tblInfo, String question) throws IOException {
		String ret = queryOpenAI(jdbcType, tblInfo, question);
		return processJSON(ret);
	}

	private static String unescape(String qCode) {
		return qCode.replace("\\\\", "\\").replace("\\t", "\t")
				.replace("\\r", "\r").replace("\\n", "\n")
				.replace("\\\"", "\"");
	}
	
	public static String queryOpenAI(JdbcTypes jdbcType, String tblInfo, String question) throws IOException {
		if(openAIkey == null || openAIkey.length() < 2) {
			throw new IllegalStateException("Open AI key isn't set");
		}
		
		// Below line is needed to make call work in bundled JRE
		// To test you must do full build and run with JRE.
		System.setProperty("https.protocols", "TLSv1.2");
		String msgs = jdbcType.isKDB() ? getKDBMessages(tblInfo, question) : getSQLMessages(tblInfo, question);
		return queryOpenAIRaw(msgs);
	}

	public static AIresult queryOpenAIstructured(String singleQuestion) throws IOException {
		String qry = singleQuestion.replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\"");
		String qryJson = "[{\"role\": \"user\", \"content\": \"" + qry + "\"}]";
		String ret = queryOpenAIRaw(qryJson);
		return processJSON(ret);
	}	
	
	
	private static String queryOpenAIRaw(String msgs) throws IOException {
		
//		String msgs = getKDBMessages(tblInfo, question) ;
		final String jsonInputString = "{\r\n" + "\r\n" + "    \"model\": \"gpt-3.5-turbo\",\r\n" + "\r\n"
				+ "    \"messages\": " + msgs + "\r\n" + "  }";
		final String url = "https://api.openai.com/v1/chat/completions";
		HttpURLConnection urlConnection = (HttpURLConnection) ((new URL(url).openConnection()));
		urlConnection.setDoOutput(true);
		urlConnection.setRequestProperty("Authorization", "Bearer " + openAIkey);
		urlConnection.setRequestProperty("Accept", "application/json");
		urlConnection.setRequestProperty("Content-Type", "application/json");
		urlConnection.setRequestMethod("POST");
		urlConnection.setConnectTimeout(15000);
		urlConnection.setReadTimeout(15000);
		try (OutputStream os = urlConnection.getOutputStream()) {
			System.out.println(jsonInputString);
			byte[] input = jsonInputString.getBytes("utf-8");
			os.write(input, 0, input.length);
		}
		if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) { // success
			StringBuffer response = new StringBuffer();
			try (BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine).append("\n");
				}
			}
			return response.toString();
		}
		return "{\"error\":" + urlConnection.getResponseCode() + ", \"errmsg\":\"" + urlConnection.getResponseMessage().replace('\"', '\'')+ "\"}";
	}
	
	static AIresult processJSON(String rawJSON) {
		String firstContent = "";
		String firstCode = "";
		if(rawJSON.contains("\"errmsg\":") || rawJSON.contains("\"error\":")) {
			throw new IllegalStateException(rawJSON);
		}

		int cIdx = rawJSON.indexOf("\"content\":");
		int logIdx = rawJSON.indexOf("logprobs");
		if(cIdx > 0 && logIdx > cIdx) {
			String s = rawJSON.substring(cIdx + "\"content\":".length(), logIdx);
			int p = s.lastIndexOf("}");
			if(p > 0) {
				s = s.substring(0, p);
				if(s.indexOf("\"") >= 0 && s.lastIndexOf("\"") > s.indexOf("\"")) {
					s = s.substring(s.indexOf("\"")+1, s.lastIndexOf("\""));
				}
				firstContent = s;
				firstCode = s;
				String[] starts = new String[] { "answer:", "correct one:",  "query:" };
				String lowerS = s.toLowerCase();
				for(String st : starts) {
					if(lowerS.indexOf(st) > 0) {
						firstCode = s.substring(lowerS.indexOf(st) + st.length());
						break;
					}
				}

				int firstCidx = firstCode.indexOf("```\n");
				if(firstCidx >= 0 && firstCode.lastIndexOf("```") > firstCidx) {
					firstCode = firstCode.substring(firstCidx + "```\n".length(), firstCode.lastIndexOf("```"));
				}
			}
		}
		firstCode = unescape(firstCode);
		firstContent = unescape(firstContent);
		firstContent = wrap(firstContent, 120, "\n", false, "", "");
		return new AIresult(rawJSON, firstContent, firstCode);
	}
	
	/**Wraps a source String into a series of lines having a maximum specified length.  The source is
	 * wrapped at: spaces, horizontal tabs, system newLine characters, or a specified newLine character
	 * sequence.  Existing newLine character sequences in the source string, whether they be the system
	 * newLine or the specified newLine, are honored.  Existing whitespace (spaces and horizontal tabs)
	 * is preserved.
	 * <p>
	 * When <code>wrapLongWords</code> is true, words having a length greater than the specified
	 * <code>lineLength</code> will be broken, the specified <code>longWordBreak</code> terminator appended,
	 * and a new line initiated with the text of the specified <code>longWordLinePrefix</code> string.  The
	 * position of the break will be unceremoniously chosen such that <code>ineLength</code> is honored.
	 * One use of <code>longWordLinePrefix</code> is to effect "hanging indents"  by specifying a series of
	 * spaces for this parameter.  This parameter can contain the lineFeed character(s).  Although
	 * <code>longWordLinePrefix</code> can contain the horizontal tab character, the results are not
	 * guaranteed because no attempt is made to determine the quantity of character positions occupied by a
	 * horizontal tab.</p>
	 * 
	 * Example usage:
	 * <pre>
	 * wrap( "  A very long word is Abracadabra in my book", 11, "\n", true, "-", "  ");</pre>
	 * returns (note the effect of the single-character lineFeed):
	 * <pre>
	 *   A very
	 * long word
	 * is Abraca-
	 *   dabra in
	 * my book</pre>
	 * Whereas, the following:
	 * <pre>
	 * wrap( "  A very long word is Abracadabra in my book", 11, null, true, null, "  ");</pre>
	 * returns (due to the 2-character system linefeed):
	 * <pre>
	 *   A very
	 * long
	 * word is A
	 *   bracada
	 *   bra in
	 * my book</pre>
	 *
	 * @param src  the String to be word wrapped, may be null
	 * @param lineLength the maximum line length, including the length of <code>newLineStr</code> and, when
	 *        applicable, <code>longWordLinePrefix</code>.  If the value is insufficient to accommodate
	 *        these two parameters + 1 character, it will be increased accordingly.
	 * @param newLineStr the string to insert for a new line, or <code>null</code> to use the value
	 *        reported as the system line separator by the JVM
	 * @param wrapLongWords  when <code>false</code>, words longer than <code>wrapLength</code> will not be broken
	 * @param longWordBreak string with which to precede <code>newLineStr</code> on each line of a broken word,
	 *        excepting the last line, or <code>null</code> if this feature is not to be used
	 * @param longWordLinePrefix string with which to prefix each line of a broken word, subsequent
	 *        to the first line, or <code>null</code> if no prefix is to be used
	 * @return a line with newlines inserted, or <code>null</code> if <code>src</code> is null
	 */
	public static String wrap(String src, int lineLength, String newLineStr, boolean wrapLongWords,
			String longWordBreak, String longWordLinePrefix) {

		if (src == null)
			return null;
		String nl = newLineStr == null ? System.getProperty("line.separator") : newLineStr;
		String lwb = longWordBreak == null ? "" : longWordBreak;
		String lwprefix = longWordLinePrefix == null ? "" : longWordLinePrefix;
		int lLength = lineLength - newLineStr.length();
		if (lLength < 1) {
			lLength = 1;
		}

		// Guard for long word break or prefix that would create an infinite loop
		if (wrapLongWords && lLength - lwb.length() - lwprefix.length() < 1) {
			lLength += lwb.length() + lwprefix.length();
		}

		int remaining = lLength;
		int breakLength = lwb.length();

		Matcher m = Pattern.compile(".+?[ \\t]|.+?(?:" + nl + ")|.+?$").matcher(src);

		StringBuilder cache = new StringBuilder();

		while (m.find()) {
			String word = m.group();

			// Breakup long word
			while (wrapLongWords && word.length() > lLength) {
				cache.append(word.substring(0, remaining - breakLength)).append(lwb).append(nl);
				word = lwprefix + word.substring(remaining - breakLength);
				remaining = lLength;
			}

			// Linefeed if word exceeds remaining space
			if (word.length() > remaining) {
				cache.append(nl).append(word);
				remaining = lLength;
			} else {
				cache.append(word);
			}

			remaining -= word.length();
		}

		return cache.toString();
	}

}
