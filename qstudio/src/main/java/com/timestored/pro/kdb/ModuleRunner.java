package com.timestored.pro.kdb;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import com.google.common.base.Joiner;
import com.timestored.misc.IOUtils;
import com.timestored.qdoc.ParsedQFile;
import com.timestored.qdoc.QFileParser;

/**
 * Allows running kdb modules, e.g. debugging / profiling.
 */
class ModuleRunner {
	
	/** qunit testing module source file **/
	private static final String QUNIT_Q = "qunit.q";
	
	/** 
	 * Bug with remote java queries to kdb that functions are declared in global namespace
	 * compared to \l scripts. This puts all functions back into their namespace.
	 * Allowing them to call each other with their shorteded names.
	 */
	private static final String NSFIXER_Q = "nsfixer.q";
	/** Math module with functions for adding, finding primes etc. **/
	public static final String MATH_Q = "math.q";
	/** Example qUnit tests for the MATH_Q module **/
	public static final String MATH_TEST_Q = "mathTest.q";
	
	private static final String NL = "\r\n";

	private static String getRunUnitTestsQuery(String namespace) throws IOException {
		return getRunUnitTestsQuery(Arrays.asList(namespace));
	}
	
	private static String getRunUnitTestsQuery(Collection<String> namespaces) throws IOException {
		
		String unittest = IOUtils.toString(ModuleRunner.class, QUNIT_Q);
		
		StringBuilder sb = new StringBuilder(unittest.length() + 500);
		sb.append(unittest).append(NL);
		fixNamespace(sb, "`.qunit");
		sb.append(".qunit.runTests ");
		for(String ns : namespaces) {
			sb.append("`").append(ns);
		}
		return sb.append(NL).toString();
	}

	private static StringBuilder fixNamespace(StringBuilder sb, String ns) throws IOException {
		String nsfixer = IOUtils.toString(ModuleRunner.class, NSFIXER_Q);
		sb.append(NL).append(nsfixer).append(ns);
		return sb.append(";").append(NL);
	}

	/**
	 * Given the code for namespaces full of tests, return code to run tests on all namespaces.
	 * @param testq The code containing tests. (msut be in a namespace)
	 * @throws IllegalArgumentException If no appropriate tests / namespaces found.
	 */
	public static String getRunQUnitQuery(String testq) throws IOException {

		String fakeName = "test.q"; // needed for parser but not important
		ParsedQFile parsedQF = QFileParser.parse(testq, fakeName, fakeName);
		Collection<String> namespaces = parsedQF.getNamespaces();
		if(namespaces.isEmpty()) {
			throw new IllegalArgumentException("no namespaces found in this file");
		}
		
		String delCurrentTests = "";
		StringBuilder nsFixes = new StringBuilder();
		for(String ns : namespaces) {
			if(ns != ".") { // just in case, do not want to ruin anyones server
				delCurrentTests += "if[`" + ns + " in `$\".\",/:string key `;" +
						"delete from `" + ns + ";];" + NL;
				
				fixNamespace(nsFixes, "`" + ns);
			}
		}
		
		return delCurrentTests + NL + testq + ";" + NL + nsFixes + NL + getRunUnitTestsQuery(namespaces);
	}
	
	/**
	 * Given the code for namespaces full of functions, return itself
	 * with additional function call at end to fix namespaces. (Kdb Bug) 
	 * 
	 */
	public static String getRunScriptModuleLoad(String qcode) throws IOException {

		String fakeName = "test.q"; // needed for parser but not important
		ParsedQFile parsedQF = QFileParser.parse(qcode, fakeName, fakeName);
		Collection<String> namespaces = parsedQF.getNamespaces();
		if(namespaces.isEmpty()) {
			return qcode;
		}

		StringBuilder nsFixes = new StringBuilder();
		for(String ns : namespaces) {
			fixNamespace(nsFixes, "`" + ns);
		}
		
		String nsList = Joiner.on("`").join(namespaces);
		return qcode + ";" + NL + nsFixes + NL + "\"Loaded Module: " + nsList + "\"";
	}
	
	/**
	 * @return Q code for a math module that allows adding, finding primes etc.
	 * @throws IOException Problem opening source code file.
	 */
	public static String getMathModule() throws IOException {
		return IOUtils.toString(ModuleRunner.class, MATH_Q);
	}

	
	/**
	 * @return Q code for qunit testing a math module.
	 * @throws IOException Problem opening source code file.
	 */
	public static String getMathModuleExampleTests() throws IOException {
		return IOUtils.toString(ModuleRunner.class, MATH_TEST_Q);
	}
}
