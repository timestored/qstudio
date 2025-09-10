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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.timestored.connections.JdbcTypes;

/**
 * Enhanced query engine that handles parameter substitution and query translation
 */
public class QueryEngine2 {
    
    private static final Pattern ARG_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private final QueryTranslator translator;
    
    public QueryEngine2() {
        this.translator = new QueryTranslator();
    }
    
    /**
     * Process a query with arguments for the specified database type
     * @param query the query template with {{argname}} placeholders
     * @param args map of argument names to values
     * @param jdbcType the target database type
     * @return the processed query with substituted parameters
     */
    public String processQuery(String query, Map<String, Object> args, JdbcTypes jdbcType) {
        if (query == null || query.trim().isEmpty()) {
            return query;
        }
        
        String processedQuery = query;
        
        // Replace argument placeholders
        Matcher matcher = ARG_PATTERN.matcher(query);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String argName = matcher.group(1);
            Object value = args.get(argName);
            String replacement = formatValueForDatabase(value, jdbcType);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        processedQuery = sb.toString();
        
        return processedQuery;
    }
    
    /**
     * Format a value appropriately for the target database
     * @param value the value to format
     * @param jdbcType the target database type
     * @return formatted string representation
     */
    private String formatValueForDatabase(Object value, JdbcTypes jdbcType) {
        if (value == null) {
            return "null";
        }
        
        if (value instanceof Boolean) {
            return translator.translateBoolean((Boolean) value, jdbcType);
        }
        
        if (value instanceof String) {
            return "'" + ((String) value).replace("'", "''") + "'";
        }
        
        if (value instanceof Number) {
            return value.toString();
        }
        
        // Default: convert to string and quote
        return "'" + value.toString().replace("'", "''") + "'";
    }
    
    /**
     * Validate that all required arguments are present
     * @param query the query template
     * @param args the provided arguments
     * @throws IllegalArgumentException if required arguments are missing
     */
    public void validateArguments(String query, Map<String, Object> args) {
        if (query == null) {
            return;
        }
        
        Matcher matcher = ARG_PATTERN.matcher(query);
        while (matcher.find()) {
            String argName = matcher.group(1);
            if (!args.containsKey(argName)) {
                throw new IllegalArgumentException("Missing required argument: " + argName);
            }
        }
    }
    
    /**
     * Extract argument names from a query template
     * @param query the query template
     * @return map of argument names to their detected types (best guess)
     */
    public Map<String, ArgType> extractArguments(String query) {
        Map<String, ArgType> arguments = new HashMap<>();
        
        if (query == null) {
            return arguments;
        }
        
        Matcher matcher = ARG_PATTERN.matcher(query);
        while (matcher.find()) {
            String argName = matcher.group(1);
            // Default to string type - could be enhanced with type inference
            arguments.put(argName, ArgType.STRING);
        }
        
        return arguments;
    }
}