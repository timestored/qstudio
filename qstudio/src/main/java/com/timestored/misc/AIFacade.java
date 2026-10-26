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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.timestored.connections.JdbcTypes;

import lombok.Data;

public class AIFacade {

	private static final Map<String, AIProvider> PROVIDERS = new LinkedHashMap<>();
	private static AIProvider activeProvider;

	static {
		register(new OpenAIProvider());
		register(new ClaudeProvider());
		register(new CustomAIProvider());
		activeProvider = PROVIDERS.values().iterator().next();
	}

	private static void register(AIProvider p) {
		PROVIDERS.put(p.getName(), p);
	}

	/** Configure the active provider and its credentials. Called from preferences. */
	public static void configure(String providerName, String apiKey, String model, String baseUrl) {
		AIProvider p = PROVIDERS.get(providerName);
		if (p == null) {
			// Fall back to first provider if name doesn't match
			p = PROVIDERS.values().iterator().next();
		}
		activeProvider = p;
		p.setApiKey(apiKey);
		p.setModel(model);
		p.setBaseUrl(baseUrl);
	}

	public static List<String> getProviderNames() {
		return new ArrayList<>(PROVIDERS.keySet());
	}

	public static String[] getModelOptions(String providerName) {
		AIProvider p = PROVIDERS.get(providerName);
		return p != null ? p.getModelOptions() : new String[] { "default" };
	}

	public static boolean isConfigured() {
		return activeProvider != null && activeProvider.isConfigured();
	}

	public static String getConfigurationHelp() {
		return activeProvider != null ? activeProvider.getConfigurationHelp()
				: "You must configure an AI provider in Settings -> Preferences -> AI";
	}

	// ---- SQL / KDB prompt templates ----

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

	private static List<ChatMessage> getKDBMessages(String tblInfo, String question) {
		String tbls = tblInfo != null ? tblInfo : "";
		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(ChatMessage.system(KDB_PREP + tbls));
		msgs.add(ChatMessage.user(KDB_Q1));
		msgs.add(ChatMessage.assistant(KDB_A1));
		msgs.add(ChatMessage.user(KDB_Q2));
		msgs.add(ChatMessage.assistant(KDB_A2));
		msgs.add(ChatMessage.user(KDB_Q3));
		msgs.add(ChatMessage.assistant(KDB_A3));
		msgs.add(ChatMessage.user("Question: " + question));
		msgs.add(ChatMessage.assistant("Answer: "));
		return msgs;
	}

	private static List<ChatMessage> getSQLMessages(String tblInfo, String question) {
		String tbls = tblInfo != null ? tblInfo : "";
		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(ChatMessage.system(SQL_PREP + tbls));
		msgs.add(ChatMessage.user(SQL_Q1));
		msgs.add(ChatMessage.assistant(SQL_A1));
		msgs.add(ChatMessage.user(SQL_Q2));
		msgs.add(ChatMessage.assistant(SQL_A2));
		msgs.add(ChatMessage.user(SQL_Q3));
		msgs.add(ChatMessage.assistant(SQL_A3));
		msgs.add(ChatMessage.user("Question: " + question));
		msgs.add(ChatMessage.assistant("Answer: "));
		return msgs;
	}

	// ---- Public query methods ----

	@Data
	public static class AIresult {
		private final String jsonReturned;
		private final String firstContent;
		private final String firstCode;
	}

	public static AIresult queryAIstructured(JdbcTypes jdbcType, String tblInfo, String question) throws IOException {
		List<ChatMessage> msgs = jdbcType.isKDB() ? getKDBMessages(tblInfo, question) : getSQLMessages(tblInfo, question);
		String response = activeProvider.chat(msgs);
		return processResponse(response);
	}

	public static AIresult queryAIstructured(String singleQuestion) throws IOException {
		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(ChatMessage.user(singleQuestion));
		String response = activeProvider.chat(msgs);
		return processResponse(response);
	}

	static AIresult processResponse(String responseText) {
		String firstContent = responseText;
		String firstCode = responseText;

		String[] starts = new String[] { "answer:", "correct one:", "query:" };
		String lowerS = firstCode.toLowerCase();
		for (String st : starts) {
			if (lowerS.indexOf(st) > 0) {
				firstCode = firstCode.substring(lowerS.indexOf(st) + st.length());
				break;
			}
		}

		int firstCidx = firstCode.indexOf("```\n");
		if (firstCidx >= 0 && firstCode.lastIndexOf("```") > firstCidx) {
			firstCode = firstCode.substring(firstCidx + "```\n".length(), firstCode.lastIndexOf("```"));
		}
		// Also handle ```sql or ```q fenced blocks
		if (firstCode.equals(responseText)) {
			int langIdx = firstCode.indexOf("```");
			if (langIdx >= 0) {
				int nlAfterFence = firstCode.indexOf('\n', langIdx);
				int closingFence = firstCode.lastIndexOf("```");
				if (nlAfterFence >= 0 && closingFence > nlAfterFence) {
					firstCode = firstCode.substring(nlAfterFence + 1, closingFence);
				}
			}
		}

		firstCode = firstCode.trim();
		firstContent = wrap(firstContent, 120, "\n", false, "", "");
		return new AIresult(responseText, firstContent, firstCode);
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
	 * @param src  the String to be word wrapped, may be null
	 * @param lineLength the maximum line length, including the length of <code>newLineStr</code> and, when
	 *        applicable, <code>longWordLinePrefix</code>.
	 * @param newLineStr the string to insert for a new line, or <code>null</code> to use the system line separator
	 * @param wrapLongWords  when <code>false</code>, words longer than <code>wrapLength</code> will not be broken
	 * @param longWordBreak string with which to precede <code>newLineStr</code> on each line of a broken word
	 * @param longWordLinePrefix string with which to prefix each line of a broken word, subsequent to the first line
	 * @return a line with newlines inserted, or <code>null</code> if <code>src</code> is null
	 */
	public static String wrap(String src, int lineLength, String newLineStr, boolean wrapLongWords,
			String longWordBreak, String longWordLinePrefix) {

		if (src == null)
			return null;
		String nl = newLineStr == null ? System.getProperty("line.separator") : newLineStr;
		String lwb = longWordBreak == null ? "" : longWordBreak;
		String lwprefix = longWordLinePrefix == null ? "" : longWordLinePrefix;
		int lLength = lineLength - nl.length();
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
