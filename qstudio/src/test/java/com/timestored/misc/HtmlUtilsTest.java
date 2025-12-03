package com.timestored.misc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;


public class HtmlUtilsTest {

	@Before public void setUp() throws Exception {}

	@After public void tearDown() throws Exception {}

	@Test public void testToListListOfString() {
		String actHtml = HtmlUtils.toList(ImmutableList.of("a","b"));
		assertTrue(actHtml.contains("<li>a"));
		assertTrue(actHtml.contains("<li>b"));
		assertTrue(actHtml.contains("<ul>"));
	}

	@Test public void testExtractBody() {
		checkEquals("abc");
		checkEquals("<p>\"<body><html></body><html></body>\"<p>");
		
	}

	private void checkEquals(String expected) {
		String htmlDoc = "<html><body>" + expected + "</body></html>";
		String actual = HtmlUtils.extractBody(htmlDoc);
		System.out.println("expected = " + expected);
		System.out.println("actual = " + actual);
		assertEquals(expected, actual);
	}

}
