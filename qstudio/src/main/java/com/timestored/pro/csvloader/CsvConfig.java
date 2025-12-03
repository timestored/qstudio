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
package com.timestored.pro.csvloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.io.Files;

/**
 * Configuration for parsing CSV files. 
 */
class CsvConfig {

	private String charset = "UTF-8";
	private char separator = ',';
	private char quote = '\"';
	private boolean containsHeader = true;
	private int skipLines = 0;
	
	String getCharset() { 	return charset; }
	char getSeparator() { return separator; }
	char getQuote() { 	return quote; }
	boolean containsHeader() { return containsHeader; }
	int getSkipLines() { 	return skipLines; }

	CsvConfig setCharset(String charset) {
		this.charset = charset; return this;
	}
	CsvConfig setSeparator(char separator) {
		this.separator = separator; return this;
	}
	CsvConfig setQuote(char quote) {
		this.quote = quote; return this;
	}
	CsvConfig setContainsHeader(boolean containsHeader) {
		this.containsHeader = containsHeader; return this;
	}
	CsvConfig setSkipLines(int skipLines) {
		this.skipLines = skipLines; return this;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("charset", charset)
			.add("separator", separator)
			.add("quote", quote)
			.add("containsHeader", containsHeader)
			.add("skipLines", skipLines)
			.toString();
	}
	
	@Override
	public int hashCode(){
		return Objects.hashCode(charset, separator, quote, containsHeader, skipLines);
	}
	
	@Override
	public boolean equals(Object object){
		if (object instanceof CsvConfig) {
			CsvConfig that = (CsvConfig) object;
			return Objects.equal(this.charset, that.charset)
				&& Objects.equal(this.separator, that.separator)
				&& Objects.equal(this.quote, that.quote)
				&& Objects.equal(this.containsHeader, that.containsHeader)
				&& Objects.equal(this.skipLines, that.skipLines);
		}
		return false;
	}
	

	/**
	 * Given a comma/pipe separated file automatically guess at it's
	 * configuration based on reading the  first 2048 chars
	 */
	public static CsvConfig detect(File csvFile) throws IOException {
		
		CsvConfig csvConfig =  new CsvConfig();
		BufferedReader r = Files.newReader(csvFile, Charset.defaultCharset());
		
		char[] b = new char[2048];
		r.read(b);
		r.close();
		
		// any pipes use it as separator
		for(char c : b) {
			if(c == '|') {
				csvConfig.setSeparator('|');
				break;
			}
		}
		
		// assume if the first line has a dot its not a header
		// safest way I can think of
		for(char c : b) {
			if(c == '\n') {
				break;
			}
			if(c == '.') {
				csvConfig.setContainsHeader(false);
			}
		}
		
		return csvConfig;
	}
	

	public CSVReader getCsvReader(File f)
			throws UnsupportedEncodingException, FileNotFoundException {
		// duplicate full set of settings of CSV file format
		CSVReader reader = new CSVReader(new InputStreamReader(
				new FileInputStream(f), getCharset()),
				getSeparator(), getQuote(),
				getSkipLines());
		return reader;
	}
}