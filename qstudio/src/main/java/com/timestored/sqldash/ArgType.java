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

/**
 * Represents the types of arguments that can be passed to queries
 */
public enum ArgType {
    
    STRING("string"),
    INTEGER("int"),
    LONG("long"),
    DOUBLE("double"),
    BOOLEAN("boolean");
    
    private final String typeName;
    
    ArgType(String typeName) {
        this.typeName = typeName;
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    /**
     * @return true if this type is a boolean type
     */
    public boolean isBoolean() {
        return this == BOOLEAN;
    }
    
    /**
     * @return true if this type is a numeric type
     */
    public boolean isNumeric() {
        return this == INTEGER || this == LONG || this == DOUBLE;
    }
    
    /**
     * Convert a Java object to this ArgType if possible
     * @param value the value to convert
     * @return true if the value can be converted to this type
     */
    public boolean canConvert(Object value) {
        if (value == null) {
            return false;
        }
        
        switch (this) {
            case STRING:
                return true; // anything can be converted to string
            case INTEGER:
                return value instanceof Integer || value instanceof Short || value instanceof Byte;
            case LONG:
                return value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte;
            case DOUBLE:
                return value instanceof Double || value instanceof Float || value instanceof Long || 
                       value instanceof Integer || value instanceof Short || value instanceof Byte;
            case BOOLEAN:
                return value instanceof Boolean;
            default:
                return false;
        }
    }
    
    @Override
    public String toString() {
        return typeName;
    }
}