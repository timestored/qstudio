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

import com.timestored.connections.JdbcTypes;

/**
 * Test class for QueryTranslator functionality
 */
public class QueryTranslatorTest {
    
    private final QueryTranslator translator = new QueryTranslator();
    
    /**
     * Test boolean translation for KDB
     */
    public void testBooleanTranslationKdb() {
        // Test true values
        String result = translator.translateBoolean(true, JdbcTypes.KDB);
        assert "1b".equals(result) : "Expected '1b' for true in KDB, got: " + result;
        
        // Test false values  
        result = translator.translateBoolean(false, JdbcTypes.KDB);
        assert "0b".equals(result) : "Expected '0b' for false in KDB, got: " + result;
        
        // Test null values
        result = translator.translateBoolean(null, JdbcTypes.KDB);
        assert "null".equals(result) : "Expected 'null' for null in KDB, got: " + result;
    }
    
    /**
     * Test boolean translation for SQL databases
     */
    public void testBooleanTranslationSql() {
        // Test with PostgreSQL
        String result = translator.translateBoolean(true, JdbcTypes.POSTGRES);
        assert "TRUE".equals(result) : "Expected 'TRUE' for true in SQL, got: " + result;
        
        result = translator.translateBoolean(false, JdbcTypes.POSTGRES);
        assert "FALSE".equals(result) : "Expected 'FALSE' for false in SQL, got: " + result;
        
        // Test with MySQL
        result = translator.translateBoolean(true, JdbcTypes.MYSQL);
        assert "TRUE".equals(result) : "Expected 'TRUE' for true in MySQL, got: " + result;
        
        result = translator.translateBoolean(false, JdbcTypes.MYSQL);
        assert "FALSE".equals(result) : "Expected 'FALSE' for false in MySQL, got: " + result;
        
        // Test null values
        result = translator.translateBoolean(null, JdbcTypes.POSTGRES);
        assert "null".equals(result) : "Expected 'null' for null in SQL, got: " + result;
    }
    
    /**
     * Test specific KDB boolean translation methods
     */
    public void testBooleanToKdb() {
        String result = translator.translateBooleanToKdb(true);
        assert "1b".equals(result) : "Expected '1b' for true, got: " + result;
        
        result = translator.translateBooleanToKdb(false);
        assert "0b".equals(result) : "Expected '0b' for false, got: " + result;
        
        result = translator.translateBooleanToKdb(null);
        assert "0Nb".equals(result) : "Expected '0Nb' for null, got: " + result;
    }
    
    /**
     * Test specific SQL boolean translation methods
     */
    public void testBooleanToSql() {
        String result = translator.translateBooleanToSql(true);
        assert "TRUE".equals(result) : "Expected 'TRUE' for true, got: " + result;
        
        result = translator.translateBooleanToSql(false);
        assert "FALSE".equals(result) : "Expected 'FALSE' for false, got: " + result;
        
        result = translator.translateBooleanToSql(null);
        assert "NULL".equals(result) : "Expected 'NULL' for null, got: " + result;
    }
    
    /**
     * Test argval boolean function
     */
    public void testArgvalBoolean() {
        Boolean result = argval.b(true);
        assert Boolean.TRUE.equals(result) : "Expected Boolean.TRUE, got: " + result;
        
        result = argval.b(false);
        assert Boolean.FALSE.equals(result) : "Expected Boolean.FALSE, got: " + result;
    }
    
    /**
     * Test integer translation for different databases
     */
    public void testIntegerTranslation() {
        // KDB integer
        String result = translator.translateInteger(42, JdbcTypes.KDB);
        assert "42i".equals(result) : "Expected '42i' for KDB integer, got: " + result;
        
        // SQL integer
        result = translator.translateInteger(42, JdbcTypes.POSTGRES);
        assert "42".equals(result) : "Expected '42' for SQL integer, got: " + result;
        
        // Null values
        result = translator.translateInteger(null, JdbcTypes.KDB);
        assert "0Ni".equals(result) : "Expected '0Ni' for null KDB integer, got: " + result;
        
        result = translator.translateInteger(null, JdbcTypes.POSTGRES);
        assert "NULL".equals(result) : "Expected 'NULL' for null SQL integer, got: " + result;
    }
    
    /**
     * Test long translation for different databases
     */
    public void testLongTranslation() {
        // KDB long
        String result = translator.translateLong(123456789L, JdbcTypes.KDB);
        assert "123456789j".equals(result) : "Expected '123456789j' for KDB long, got: " + result;
        
        // SQL long
        result = translator.translateLong(123456789L, JdbcTypes.MYSQL);
        assert "123456789".equals(result) : "Expected '123456789' for SQL long, got: " + result;
    }
    
    /**
     * Test double translation for different databases
     */
    public void testDoubleTranslation() {
        // KDB double
        String result = translator.translateDouble(3.14159, JdbcTypes.KDB);
        assert "3.14159f".equals(result) : "Expected '3.14159f' for KDB double, got: " + result;
        
        // SQL double
        result = translator.translateDouble(3.14159, JdbcTypes.POSTGRES);
        assert "3.14159".equals(result) : "Expected '3.14159' for SQL double, got: " + result;
    }
    
    /**
     * Run all tests
     */
    public static void runAllTests() {
        QueryTranslatorTest test = new QueryTranslatorTest();
        
        try {
            test.testBooleanTranslationKdb();
            System.out.println("✓ Boolean KDB translation tests passed");
            
            test.testBooleanTranslationSql();
            System.out.println("✓ Boolean SQL translation tests passed");
            
            test.testBooleanToKdb();
            System.out.println("✓ Boolean to KDB tests passed");
            
            test.testBooleanToSql();
            System.out.println("✓ Boolean to SQL tests passed");
            
            test.testArgvalBoolean();
            System.out.println("✓ Argval boolean tests passed");
            
            test.testIntegerTranslation();
            System.out.println("✓ Integer translation tests passed");
            
            test.testLongTranslation();
            System.out.println("✓ Long translation tests passed");
            
            test.testDoubleTranslation();
            System.out.println("✓ Double translation tests passed");
            
            System.out.println("\n✓ All QueryTranslatorTest tests passed!");
            
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