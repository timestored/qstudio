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
package com.timestored.command;

import java.awt.EventQueue;
import java.util.Collection;
import java.util.List;

import javax.swing.KeyStroke;

import com.google.common.collect.Lists;
import com.timestored.docs.OpenDocumentsModel;
import com.timestored.qstudio.model.QueryManager;
import com.timestored.theme.Theme;

import lombok.AllArgsConstructor;

/**
 * Converts CodeSnippets to runnable commands and "provides them".
 */
public class CodeSnippetCommandProvider implements CommandProvider {

	private final OpenDocumentsModel openDocsModel;
	private final QueryManager queryManager;
	private final List<CodeSnippet> snippets = Lists.newArrayList();
	
	private static final String getProcessDetails = "{ t:(update d:`port`console`errorTrap`GC`precision`slaves`timer`Timeout`GMToffset`weekOffset`dateMode,v:system each s from ([] s:\"pcegPstToWz\"));\r\n" + 
			"    u:( [] s:`.z.a`.z.h`.z.i`.z.k`.z.K`.z.l`.z.o`.z.u`.z.x`.z.X);\r\n" + 
			"    u:update d:`ip`hostname`PID`releaseDate`releaseVersion`license`os`username`cmd`cmdLine,v:value each s from u;\r\n" + 
			"    u:update {\".\" sv string `int$0x00 vs x} each v from u where d=`ip;\r\n" + 
			"    `s xkey `s xasc t uj u}[]";
	
	private static final String getCountByForOneTable = "// Get count by date and column for one table\r\n" + 
			"{ t:?[x;();`date`s!`date,y;enlist[`cn]!enlist (count;`i)];\r\n" + 
			" P:`$string asc distinct (0!t)`s;\r\n" + 
			" exec P!((`$string s)!cn)P by date:date from t}[quote;`cond]";
	
	private static final String getHDBcounts = "(uj/) {(.Q.pf,x) xcol ?[x;enlist (in;.Q.pf;-20 sublist value .Q.pf);{x!x}(),.Q.pf;enlist[`cn]!enlist (count;(cols x) 1)]} each .Q.pt";
	
	private static final String getTableCounts = "// Get overview of  all tables\r\n" + 
			"{ [] \r\n" + 
			"    gett:{\r\n" + 
			"    	// replace empty wqith blank, 1 dont show comma, many use -3!\r\n" + 
			"    	formatVals:{@[{$[0=count x;\"\";$[(11h=type x) and 1=count x;\"`\",string first x;80 sublist trim -3!`#x]]};x;\"  \"]};\r\n" + 
			"        safeCount: {$[.Q.qp x; $[`pn in key `.Q; {$[count x;sum x;0N]} .Q.pn y; -1]; count x]};\r\n" + 
			"    	getFmt:{((0;0b;1b)!`memory`splayed`partitioned) .Q.qp[value x]};\r\n" + 
			"    	cnames:`table`count`format`columns`keys;\r\n" + 
			"    	cnames!(x; safeCount[value x;x]; getFmt x;formatVals asc cols x; k:asc keys x)};\r\n" + 
			"    `table xkey gett each asc system \"a\"}[]";
	
	
	public CodeSnippetCommandProvider(OpenDocumentsModel openDocsModel, QueryManager queryManager) {
		this.openDocsModel = openDocsModel;
		this.queryManager = queryManager;
		
		snippets.add(new CodeSnippet("getProcessDetails", getProcessDetails, true, false));
		snippets.add(new CodeSnippet("getHDBcounts", getHDBcounts, true, false));
		snippets.add(new CodeSnippet("getTableCounts", getTableCounts, true, false));
		snippets.add(new CodeSnippet("getCountByForOneTable", getCountByForOneTable, false, true));
	}

	@Override public Collection<Command> getCommands() {
		final List<Command> commands = Lists.newArrayList();
		for(CodeSnippet snip : snippets) {
			if(snip.isPasteable()) {
				commands.add(new PasteSnippetCommand(snip));
			}
			if(snip.isRunnable()) {
				commands.add(new RunSnippetCommand(snip));
			}
		}
		return commands;
	}

	@AllArgsConstructor
	class PasteSnippetCommand implements Command {
		protected final CodeSnippet codeSnippet;
		
		@Override public javax.swing.Icon getIcon() { return Theme.CIcon.EDIT_PASTE.get16(); }
		@Override public String getTitle() { return "Paste Snip: " + codeSnippet.getName(); }
		@Override public String getDetailHtml() { return codeSnippet.getName(); }
		@Override public KeyStroke getKeyStroke() { return (KeyStroke) null; }
		@Override public String toString() { return getTitle(); };
		@Override public String getTitleAdditional() { return ""; }
		@Override public void perform() {
			EventQueue.invokeLater(new Runnable() {
				@Override public void run() {openDocsModel.insertSelectedText(codeSnippet.getCode());}
			});
			 
		}
	}

	class RunSnippetCommand extends PasteSnippetCommand {
		public RunSnippetCommand(final CodeSnippet codeSnippet) {
			super(codeSnippet);
		}
		@Override public javax.swing.Icon getIcon() { return Theme.CIcon.CLOCK_GO.get16(); }
		@Override public String getTitle() { return "Run Snip: " + codeSnippet.getName(); }
		@Override public void perform() { queryManager.sendQuery(codeSnippet.getCode(), getTitle()); }
	}
}
