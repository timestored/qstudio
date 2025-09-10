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
 * Translates values between different database formats, particularly for KDB and SQL databases
 */
public class QueryTranslator {
    
    /**
     * Translate a boolean value to the appropriate string representation for the target database
     * @param value the boolean value to translate
     * @param jdbcType the target database type
     * @return string representation appropriate for the database
     */
    public String translateBoolean(Boolean value, JdbcTypes jdbcType) {
        if (value == null) {
            return "null";
        }
        
        if (jdbcType.isKDB()) {
            // KDB should produce 1b or 0b
            return value ? "1b" : "0b";
        } else {
            // SQL should be TRUE or FALSE
            return value ? "TRUE" : "FALSE";
        }
    }
    
    /**
     * Translate a boolean value to KDB format
     * @param value the boolean value
     * @return "1b" for true, "0b" for false, "0Nb" for null
     */
    public String translateBooleanToKdb(Boolean value) {
        if (value == null) {
            return "0Nb"; // KDB null boolean
        }
        return value ? "1b" : "0b";
    }
    
    /**
     * Translate a boolean value to SQL format
     * @param value the boolean value
     * @return "TRUE" for true, "FALSE" for false, "NULL" for null
     */
    public String translateBooleanToSql(Boolean value) {
        if (value == null) {
            return "NULL";
        }
        return value ? "TRUE" : "FALSE";
    }
    
    /**
     * Translate an integer value for the target database
     * @param value the integer value
     * @param jdbcType the target database type
     * @return string representation appropriate for the database
     */
    public String translateInteger(Integer value, JdbcTypes jdbcType) {
        if (value == null) {
            return jdbcType.isKDB() ? "0Ni" : "NULL";
        }
        
        if (jdbcType.isKDB()) {
            return value.toString() + "i";
        } else {
            return value.toString();
        }
    }
    
    /**
     * Translate a long value for the target database
     * @param value the long value
     * @param jdbcType the target database type
     * @return string representation appropriate for the database
     */
    public String translateLong(Long value, JdbcTypes jdbcType) {
        if (value == null) {
            return jdbcType.isKDB() ? "0Nj" : "NULL";
        }
        
        if (jdbcType.isKDB()) {
            return value.toString() + "j";
        } else {
            return value.toString();
        }
    }
    
    /**
     * Translate a double value for the target database
     * @param value the double value
     * @param jdbcType the target database type
     * @return string representation appropriate for the database
     */
    public String translateDouble(Double value, JdbcTypes jdbcType) {
        if (value == null) {
            return jdbcType.isKDB() ? "0Nf" : "NULL";
        }
        
        if (jdbcType.isKDB()) {
            return value.toString() + "f";
        } else {
            return value.toString();
        }
    }
    
    /**
     * Translate a string value for the target database
     * @param value the string value
     * @param jdbcType the target database type
     * @return string representation appropriate for the database
     */
    public String translateString(String value, JdbcTypes jdbcType) {
        if (value == null) {
            return "NULL";
        }
        
        if (jdbcType.isKDB()) {
            // KDB string literal
            return "`" + value.replace("`", "``");
        } else {
            // SQL string literal
            return "'" + value.replace("'", "''") + "'";
        }
    }
    
    /**
     * Translate any object to the appropriate format for the target database
     * @param value the value to translate
     * @param jdbcType the target database type
     * @return string representation appropriate for the database
     */
    public String translateValue(Object value, JdbcTypes jdbcType) {
        if (value == null) {
            return "NULL";
        }
        
        if (value instanceof Boolean) {
            return translateBoolean((Boolean) value, jdbcType);
        }
        
        if (value instanceof Integer) {
            return translateInteger((Integer) value, jdbcType);
        }
        
        if (value instanceof Long) {
            return translateLong((Long) value, jdbcType);
        }
        
        if (value instanceof Double || value instanceof Float) {
            return translateDouble(((Number) value).doubleValue(), jdbcType);
        }
        
        if (value instanceof String) {
            return translateString((String) value, jdbcType);
        }
        
        // Default: convert to string and translate as string
        return translateString(value.toString(), jdbcType);
    }
}