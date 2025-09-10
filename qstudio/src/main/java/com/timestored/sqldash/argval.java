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
 * Utility class for creating argument values of various types
 */
public class argval {
    
    /**
     * Create a boolean argument value
     * @param bflag the boolean flag
     * @return Boolean wrapper for the flag
     */
    public static Boolean b(boolean bflag) {
        return Boolean.valueOf(bflag);
    }
    
    /**
     * Create a string argument value
     * @param str the string value
     * @return String value
     */
    public static String s(String str) {
        return str;
    }
    
    /**
     * Create an integer argument value
     * @param i the integer value
     * @return Integer wrapper for the value
     */
    public static Integer i(int i) {
        return Integer.valueOf(i);
    }
    
    /**
     * Create a long argument value
     * @param l the long value
     * @return Long wrapper for the value
     */
    public static Long l(long l) {
        return Long.valueOf(l);
    }
    
    /**
     * Create a double argument value
     * @param d the double value
     * @return Double wrapper for the value
     */
    public static Double d(double d) {
        return Double.valueOf(d);
    }
    
    /**
     * Create a float argument value
     * @param f the float value
     * @return Float wrapper for the value
     */
    public static Float f(float f) {
        return Float.valueOf(f);
    }
}