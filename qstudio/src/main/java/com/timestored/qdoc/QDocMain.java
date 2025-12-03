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

package com.timestored.qdoc;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.timestored.docs.Document;

/**
 * Allows running qDoc on files / directories from the command line. 
 * Command line arguments are: 
 * 1-targetFolder- created if necessary 
 * 2-qSrcFolder searched recursively for .q files
 * 3-(optional) baseWeblink A http web address specifying a kdb server against which example call links should be generated.
 */
public class QDocMain {

	public static void main(String... args) {
		
		String baseWeblink = null;
		/* Check args  */
		if(args.length < 2) {
			printErr("", 1);
		}
		if(args.length >= 3) {
			baseWeblink = args[2];
		}
		File target = new File(args[0]);
		boolean suc = target.mkdirs();
		if(!target.isDirectory()) {
			printErr("target folder must be writeable directory: " + args[0], 1);
		}
		
		File qSrc = new File(args[1]);
		if(!qSrc.isDirectory()) {
			printErr("source folder must be directory:" + args[1], 2);
		}
		
		/* Get all .q docs */
		Collection<File> qs = listFileTree(qSrc, ".q");
		List<Document> docs = Lists.newArrayList();
		for(File qFile : qs) {
			try {
				docs.add(new Document(qFile));
				System.out.println("Processed: " + qFile);
			} catch (IOException e) {
				System.err.println("Problem with file:" + qFile);
			}
		}
		
		/* Generate docs and output any errors */
		List<String> errors = HtmlPqfOutputter.output(docs, target, baseWeblink);
		for(String e : errors) {
			System.err.println("Error: " + e);
		}
	}

	private static void printErr(String msg, int num) {
		System.err.println("Must specify 2 args. 1-targetFolder, 2-qSrcFolder searched recursively");
		if(msg.length()>0) {
			System.err.println(msg);
		}
		System.exit(num);
	}
	
	private static Collection<File> listFileTree(File dir, String suffix) {
		if(dir.isDirectory()) {
		    Set<File> fileTree = new HashSet<File>();
		    for (File entry : dir.listFiles()) {
		    	if(entry != null) {
			        if (entry.isFile() && (suffix==null || entry.getName().endsWith(suffix))) { 
			        	fileTree.add(entry);
			        } else { 
			        	fileTree.addAll(listFileTree(entry, suffix));
			        }
		    	}
		    }
		    return fileTree;
		}
		return Collections.emptySet();
	}
}
