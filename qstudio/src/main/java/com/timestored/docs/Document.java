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
package com.timestored.docs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import lombok.Setter;

import static com.google.common.base.MoreObjects.toStringHelper;
import com.google.common.base.Preconditions;
import com.timestored.misc.IOUtils;
import com.timestored.theme.Icon;
import com.timestored.theme.Theme;



/**
 * Represents a saveable file with current line, selected text and  saved status.
 */
public class Document {

	private static final Logger LOG = Logger.getLogger(Document.class.getName());
	private static AtomicInteger counter = new AtomicInteger(0);

	private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
	
	private String title;
	@Setter private File file = null;
	private String savedContent = "";
	private String content = "";
	/*** value of currently selected line */
	private int selectionStart = 0;
	private int selectionEnd = 0;
	private int caretPosition = 0;
	
	public Document() {
		int v = counter.incrementAndGet();
		this.title = "new "+v;
	}
	
	public Document(File file) throws IOException {		
		this.file = Preconditions.checkNotNull(file);
        content = IOUtils.toString(file, Charset.forName("UTF-8")).replace("\r", "");
        savedContent = content;
		this.title = file.getName();
	}

	public void reloadFromFile() throws IOException {
		if(file != null) {
	        String s = IOUtils.toString(file, Charset.forName("UTF-8")).replace("\r", "");
	        setContent(s);
		}
		
	}
	
	/** true if this document has been changed and not saved */
	public boolean hasUnsavedChanges() {
		return !savedContent.equals(content);
	}

	/** true if this document has been changed and not saved */
	public boolean isReadOnly() {
		return file!=null && !file.canWrite();
	}
	
	/** the absolute path where this was last saved to or null if never saved */
	public String getFilePath() {
		return file==null ? null : file.getAbsolutePath();
	}
	
	/**The file ending e.g. txt or empty "" if file ending is not known. Never null */
	public String getFileEnding() {
		String p = file==null ? "" : file.getAbsolutePath().toLowerCase();
        int dotPos = p.lastIndexOf('.');
        return dotPos >= 0 ? p.substring(dotPos+1) : "";
	}

	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }

	@Override
	public String toString() {
		return toStringHelper(this)
			.add("title", title)
			.add("file", file)
			.add("selectionStart", selectionStart)
			.add("selectionEnd", selectionEnd)
			.add("unsavedChanges", hasUnsavedChanges())
			.toString();
	}
	
	/**
	 * @return true if this document is saved as a file ending in .k or .q
	 */
	public boolean isQKfileSuffix() {
		
		if(file != null) {
			String fp = file.getName().toUpperCase();
			return (fp.endsWith(".Q") || fp.endsWith(".K"));
		}
		return false;
	}

	public String getContent() {
		return content;
	}
	
	
	public int getSelectionEnd() {
		return selectionEnd;
	}
	
	
	public int getSelectionStart() {
		return selectionStart;
	}

	/**
	 * @return Currently selected text if there is any, otherwise empty string
	 */
	public String getSelectedText() {
		return content.substring(selectionStart, selectionEnd);
	}

	/**
	 * @return true if some text is selected, false otherwise.
	 */
	public boolean isTextSelected() {
		return selectionEnd>selectionStart;
	}
	
	/**
	 * @return Get the line of text for the current cursor position
	 */
	public String getCurrentLine() {
		final char LS ='\n';
		int i= Math.max(caretPosition-1, 0);
		while(i>0 && i<content.length() && content.charAt(i)!=LS) {
			i--;
		}
		int j=Math.min(caretPosition, content.length());
		while(j<content.length() && content.charAt(j)!=LS) {
			j++;
		}
		return content.substring(i, j).trim();
	}

	public int[] getCurrentStatementBounds() {
		return getStatementBounds(content, caretPosition);
	}
	
	public String getCurrentStatement() {
		int[] r = getCurrentStatementBounds();
		return content.substring(r[0], r[1]);
	}

	public static int[] getStatementBounds(String content, int caretPosition) {
		List<Integer> splitPoints = getSqlStatementSplitpoints(content);
		if(splitPoints.isEmpty()) {
			return new int[] { 0, content.length() };
		}
		int idx = 0;
		while(idx < splitPoints.size() && splitPoints.get(idx) < caretPosition) {
			idx++;
		}
		int start = idx > 0 ? splitPoints.get(idx-1)+1 : 0;
		int end = idx < splitPoints.size() ? splitPoints.get(idx) : content.length();
		return new int[] { start, end };
	}

	public static List<Integer> getSqlStatementSplitpoints(String sqlCode) {
		if(sqlCode == null || sqlCode.isEmpty() || sqlCode.trim().length()==0) {
			return Collections.emptyList();
		}
		List<Integer> splitPoints = new ArrayList<>();
		boolean textFound = false;
		for(int i=0; i<sqlCode.length(); i++) {
			char c = sqlCode.charAt(i);
			char nextC = i < sqlCode.length()-1 ? sqlCode.charAt(i+1) : ' ';
			switch(c) {
			case ';': splitPoints.add(i); textFound = false; break;
			case '-':
				if(nextC == '-') {
					i++; i++;
					while(i<sqlCode.length() && sqlCode.charAt(i) != '\n') {
						i++;
					}
					if(!textFound) {
						splitPoints.add(i);
					}
				} else {
					textFound = true;
				}
				break;
			case '`':
				if(nextC == '`' && i<sqlCode.length()-2 && sqlCode.charAt(i+2)=='`') {
					i++; i++; i++;
					while(i<sqlCode.length() && sqlCode.charAt(i) != '\n') {
						i++;
					}
					splitPoints.add(i);
					textFound = false;
					break;
				}
				break;
			case '/':
				if(nextC == '*') {
					i++;
					for(i++; i<sqlCode.length(); i++) {
						if(sqlCode.charAt(i-1)=='*' && sqlCode.charAt(i)=='/') {
							i++;
							if(!textFound) {
								splitPoints.add(i);
							}
							break;
						}
					}
					//splitPoints.add(i); // break so that running statement within comment does NOT run statement
				}
				textFound = true;
				break;
			case '\\': // escaping next item
				i++;
				textFound = true;
				break;
			case '\"':
			case '\'':
				textFound = true;
				char insidec = c;
				for(i++; i<sqlCode.length(); i++) {
					boolean notEscaped = i==sqlCode.length() || sqlCode.charAt(i-1) != '\\';
					c = sqlCode.charAt(i);
					if((c == insidec) && notEscaped) {
						break; // find ending and swallow quotations
					} else if(c == '\n' && notEscaped) {
						splitPoints.add(i);
						break;
					}
				}
				break;
			case '\n':
			case ' ':
			case '\r':
			case '\t':
				break;
			default:
				textFound = true;
				break;
			}
		}
		return splitPoints;
	}
	
	/**
	 * @return Get text block before the carat, stopping at whitespace.
	 */
	public String getTextBeforeCarat() {
		final char LS ='\n';
		int i= Math.max(caretPosition-1, 0);
		while(i>0 && i<content.length() && content.charAt(i)!=LS 
				&& content.charAt(i)!=' ') {
			i--;
		}
		
		return content.substring(i, caretPosition).trim();
	}
	
	public void saveAs(File file, boolean useWindowsLineEndings) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")))) {
			if(useWindowsLineEndings) {
				bw.write(content.replace("\n", "\r\n"));
			} else {
				bw.write(content);
			}
			this.file = file;
			title = file.getName();
			savedContent = content;
			for(Listener l : listeners) {
				l.docSaved();
			}
		}
	}

	public void setContent(String content) {
		this.content = content.replace("\r", "");
		int l = content.length();
		if(selectionEnd > l) {
			selectionEnd = l;
		}
		if(selectionStart > l) {
			selectionStart = l;
		}
		notifyListenersContentModified();
	}
	
	public void save(boolean useWindowsLineEndings) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"))) {
			if(useWindowsLineEndings) {
				bw.write(content.replace("\n", "\r\n"));
			} else {
				bw.write(content);
			}
			savedContent = content;

			for(Listener l : listeners) {
				l.docSaved();
			}
		}
	}


	public void setSelection(int selectionStart, int selectionEnd, int caretPosition) {
		Preconditions.checkArgument((selectionStart>=0) 
				&& (selectionEnd >= selectionStart));
		Preconditions.checkArgument(selectionEnd <= content.length());
		
		this.selectionStart = selectionStart;
		this.selectionEnd = selectionEnd;
		this.caretPosition = caretPosition;
		LOG.finest("(selectionStart - selectionEnd) -> " + selectionStart + " - " + selectionEnd);
		LOG.finest("setSelection -> getSelectedText() = " + getSelectedText());
		notifyListenersCaratMoved();
	}

	public void setCaratPosition(int caratPosition) {
		
		Preconditions.checkArgument(caratPosition <= content.length() && caratPosition >= 0);
		this.caretPosition  = caratPosition;
		LOG.finest("caratPosition = " + caratPosition);
		notifyListenersCaratMoved();
	}
	
	
	public int getCaratPosition() {
		return caretPosition;
	}

	/**
	 * Insert text into the document and move the carat to the end of the text
	 */
	public void insertSelectedText(String text) {
		String t = text.replace("\r\n", "\n");
		content = content.substring(0, selectionStart) 
				+ "\n" + t + content.substring(selectionEnd);
		if(caretPosition > content.length()) {
			caretPosition = content.length();	
		}
		int pos = selectionStart + t.length() + 1;
		notifyListenersContentModified();
		setCaratPosition(pos);
	}

	/**
	 * Insert text at current carat position, Without moving carat.
	 */
	public void insertText(String text) {
		content = content.substring(0, selectionEnd) 
				+ text 
				+ content.substring(selectionEnd);
		notifyListenersContentModified();
	}

	/**
	 * Move the carat to the start of the next line if there is one.
	 */
	public void gotoNextLine() {
		int i=caretPosition;
		boolean found = false;
		while(i<content.length()-1) {
			if(content.charAt(i)=='\n') {
				found = true;
				break;
			}
			i++;
		}
		if(found) {
			setCaratPosition(i+1);
		}
	}
	
	
	/*
	 * Listener code
	 *********************************************************/
	
	/**
	 * Allows being notified of content or selection changes.
	 */
	public static interface Listener {
		/** The content was modified */
		public void docContentModified();
		/** The carat/selection has been changed */
		public void docCaratModified();
		/** The carat/selection has been changed */
		public void docSaved();
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}
	
	private void notifyListenersContentModified() {
		for(Listener l : listeners) {
			l.docContentModified();
		}
	}
	
	private void notifyListenersCaratMoved() {
		for(Listener l : listeners) {
			l.docCaratModified();
		}
	}

	public Icon getIcon() {
		Icon icon = Theme.CIcon.PAGE;
		if(isReadOnly()) {
			icon = Theme.CIcon.PAGE_WHITE_ZIP;
		} else if(hasUnsavedChanges()) {
			icon = Theme.CIcon.PAGE_RED;
		} else if(isQKfileSuffix()){
			icon = Theme.CIcon.PAGE_CODE;
		}
		return icon;
	}

	public boolean isInMemoryAndEmpty() {
		return getFilePath() == null && content.trim().isEmpty();
	}
}
	