package com.timestored.sqldash.chart;

import static org.junit.Assert.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.Test;

import com.timestored.babeldb.DBHelper;

public class TimeSeriesPivotManagerTest {

    @Test public void testPivotAppliedWhenCandidateDetected() throws SQLException {
    	ResultSet TS_SIMPLE = new SimpleResultSet(
    		    new String[] {"dt", "sym", "price"},
    		    new Object[] {
    		        new java.sql.Date[] {
    		            java.sql.Date.valueOf("2023-01-01"),
    		            java.sql.Date.valueOf("2023-01-01"),
    		            java.sql.Date.valueOf("2023-01-02"),
    		            java.sql.Date.valueOf("2023-01-02")
    		        },
    		        new String[] {"A", "B", "A", "B"},
    		        new double[] {10.0, 20.0, 11.0, 21.0}
    		    }
    		);

        ResultSet rs = TimeSeriesPivotManager.pivotIfNeeded(TS_SIMPLE);
        String EXP = "91 | 8 | 8\r\n"
        		+ "dt | A | B\r\n"
        		+ "2023-01-01 | 10.0 | 20.0\r\n"
        		+ "2023-01-02 | 11.0 | 21.0\r\n";
        assertEquals(DBHelper.toString(rs,true), EXP);
    }

    
    @Test public void testNoPivotReturnsNull() throws SQLException {
    	ResultSet TS_ALREADY_WIDE = new SimpleResultSet(
                new String[] {"dt", "A_price", "B_price"},
                new Object[] {
                    new int[] {1, 2},
                    new double[] {10.0, 11.0},
                    new double[] {20.0, 21.0}
                }
            );
        ResultSet rs = TimeSeriesPivotManager.pivotIfNeeded(TS_ALREADY_WIDE);
        assertNull("Already wide data should not be auto-pivoted", rs);
    }

    @Test public void testInvalidDataReturnsNullNotException() throws SQLException {
        ResultSet TS_INVALID = new SimpleResultSet(
                new String[] {"x", "y"},
                new Object[] {
                    new int[] {1, 2},
                    new int[] {3, 4}
                }
            );
        ResultSet rs = TimeSeriesPivotManager.pivotIfNeeded(TS_INVALID);
        assertNull("Invalid input must fail safely", rs);
    }


}
