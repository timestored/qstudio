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
package com.timestored.sqldash;

import java.util.HashMap;
import java.util.Map;

import com.timestored.connections.JdbcTypes;

/**
 * Test class for QueryEngine2 functionality
 */
public class QueryEngine2Test {
    
    private final QueryEngine2 engine = new QueryEngine2();
    
    /**
     * Test processing query with boolean arguments for KDB
     */
    public void testProcessQueryWithBooleanKdb() {
        String query = "select from table where active={{enabled}} and flag={{debug}}";
        Map<String, Object> args = new HashMap<>();
        args.put("enabled", argval.b(true));
        args.put("debug", argval.b(false));
        
        String result = engine.processQuery(query, args, JdbcTypes.KDB);
        String expected = "select from table where active=1b and flag=0b";
        
        assert expected.equals(result) : "Expected: " + expected + ", got: " + result;
    }
    
    /**
     * Test processing query with boolean arguments for SQL
     */
    public void testProcessQueryWithBooleanSql() {
        String query = "SELECT * FROM table WHERE active={{enabled}} AND flag={{debug}}";
        Map<String, Object> args = new HashMap<>();
        args.put("enabled", argval.b(true));
        args.put("debug", argval.b(false));
        
        String result = engine.processQuery(query, args, JdbcTypes.POSTGRES);
        String expected = "SELECT * FROM table WHERE active=TRUE AND flag=FALSE";
        
        assert expected.equals(result) : "Expected: " + expected + ", got: " + result;
    }
    
    /**
     * Test processing query with mixed argument types
     */
    public void testProcessQueryWithMixedArgs() {
        String query = "SELECT * FROM table WHERE active={{enabled}} AND count > {{minCount}} AND name = {{userName}}";
        Map<String, Object> args = new HashMap<>();
        args.put("enabled", argval.b(true));
        args.put("minCount", argval.i(10));
        args.put("userName", argval.s("test"));
        
        String result = engine.processQuery(query, args, JdbcTypes.POSTGRES);
        String expected = "SELECT * FROM table WHERE active=TRUE AND count > 10 AND name = 'test'";
        
        assert expected.equals(result) : "Expected: " + expected + ", got: " + result;
    }
    
    /**
     * Test processing KDB query with mixed argument types
     */
    public void testProcessQueryWithMixedArgsKdb() {
        String query = "select from table where active={{enabled}}, count > {{minCount}}, name = {{userName}}";
        Map<String, Object> args = new HashMap<>();
        args.put("enabled", argval.b(true));
        args.put("minCount", argval.i(10));
        args.put("userName", argval.s("test"));
        
        String result = engine.processQuery(query, args, JdbcTypes.KDB);
        String expected = "select from table where active=1b, count > 10, name = 'test'";
        
        assert expected.equals(result) : "Expected: " + expected + ", got: " + result;
    }
    
    /**
     * Test argument validation
     */
    public void testArgumentValidation() {
        String query = "SELECT * FROM table WHERE active={{enabled}} AND count={{minCount}}";
        Map<String, Object> args = new HashMap<>();
        args.put("enabled", argval.b(true));
        // Missing minCount argument
        
        try {
            engine.validateArguments(query, args);
            assert false : "Expected IllegalArgumentException for missing argument";
        } catch (IllegalArgumentException e) {
            assert e.getMessage().contains("minCount") : "Expected error message about minCount, got: " + e.getMessage();
        }
    }
    
    /**
     * Test argument extraction
     */
    public void testArgumentExtraction() {
        String query = "SELECT * FROM table WHERE active={{enabled}} AND count > {{minCount}} AND name = {{userName}}";
        Map<String, ArgType> args = engine.extractArguments(query);
        
        assert args.size() == 3 : "Expected 3 arguments, got: " + args.size();
        assert args.containsKey("enabled") : "Expected 'enabled' argument";
        assert args.containsKey("minCount") : "Expected 'minCount' argument";
        assert args.containsKey("userName") : "Expected 'userName' argument";
        
        // All should default to STRING type
        assert args.get("enabled") == ArgType.STRING : "Expected STRING type for enabled";
        assert args.get("minCount") == ArgType.STRING : "Expected STRING type for minCount";
        assert args.get("userName") == ArgType.STRING : "Expected STRING type for userName";
    }
    
    /**
     * Test processing query with null arguments
     */
    public void testProcessQueryWithNullArgs() {
        String query = "SELECT * FROM table WHERE active={{enabled}}";
        Map<String, Object> args = new HashMap<>();
        args.put("enabled", null);
        
        String result = engine.processQuery(query, args, JdbcTypes.POSTGRES);
        String expected = "SELECT * FROM table WHERE active=null";
        
        assert expected.equals(result) : "Expected: " + expected + ", got: " + result;
    }
    
    /**
     * Test processing query with no arguments
     */
    public void testProcessQueryWithNoArgs() {
        String query = "SELECT * FROM table WHERE 1=1";
        Map<String, Object> args = new HashMap<>();
        
        String result = engine.processQuery(query, args, JdbcTypes.POSTGRES);
        
        assert query.equals(result) : "Expected query to remain unchanged, got: " + result;
    }
    
    /**
     * Run all tests
     */
    public static void runAllTests() {
        QueryEngine2Test test = new QueryEngine2Test();
        
        try {
            test.testProcessQueryWithBooleanKdb();
            System.out.println("✓ Process query with boolean KDB tests passed");
            
            test.testProcessQueryWithBooleanSql();
            System.out.println("✓ Process query with boolean SQL tests passed");
            
            test.testProcessQueryWithMixedArgs();
            System.out.println("✓ Process query with mixed args SQL tests passed");
            
            test.testProcessQueryWithMixedArgsKdb();
            System.out.println("✓ Process query with mixed args KDB tests passed");
            
            test.testArgumentValidation();
            System.out.println("✓ Argument validation tests passed");
            
            test.testArgumentExtraction();
            System.out.println("✓ Argument extraction tests passed");
            
            test.testProcessQueryWithNullArgs();
            System.out.println("✓ Process query with null args tests passed");
            
            test.testProcessQueryWithNoArgs();
            System.out.println("✓ Process query with no args tests passed");
            
            System.out.println("\n✓ All QueryEngine2Test tests passed!");
            
        } catch (AssertionError e) {
            System.err.println("✗ Test failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Main method to run tests
     */
    public static void main(String[] args) {
        runAllTests();
    }
}