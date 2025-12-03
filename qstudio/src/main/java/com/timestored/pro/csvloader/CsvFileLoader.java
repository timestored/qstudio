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

import java.io.File;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Facade to provide simple public interface. 
 *
 */
class CsvFileLoader {
	
	/**
	 * Autodetect csv format, process all rows buffered, sending each chunk to TabListener. 
	 */
	public static void loadFile(TabListener tabListener, File f) throws IOException {

		CsvConfig csvConfig = CsvConfig.detect(f);
		CSVReader reader = csvConfig.getCsvReader(f);
		BufferedReadProc bufferedReader = new BufferedReadProc(csvConfig, 100);
		bufferedReader.addTabListener(tabListener);
		String[] values = reader.readNext();
		int row=0;
		while (values != null) {
			bufferedReader.procRow(row++, values);
			values = reader.readNext();
		}
		bufferedReader.flush();
		reader.close();
	}
}
