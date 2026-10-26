package com.timestored.sqldash.chart;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
import com.timestored.babeldb.DBHelper;


public final class TimeSeriesPivotManager {

    private static final Logger LOG = Logger.getLogger(TimeSeriesPivotManager.class.getName());

    private TimeSeriesPivotManager() {}

    public static ResultSet pivotIfNeeded(ResultSet rs) throws SQLException {
        if (rs == null) {
            return null;
        }

        try {
            AutoPivotDetector.PivotCandidate candidate = AutoPivotDetector.detectPivotColumn(rs);
            if (!candidate.shouldPivot()) {
                return null;
            }

            String timeCol = candidate.getTimeColumnName();
            String pivotCol = candidate.getPivotColumnName();

            // Sort required by PivotResultSet
            ResultSet sorted = DBHelper.sort(rs, Lists.newArrayList(timeCol, pivotCol));
            ResultSet pivoted = DBHelper.pivot(sorted, List.of(timeCol), List.of(pivotCol));
            pivoted = tryRemovePivotPostfix(pivoted, timeCol);
            return pivoted;

        } catch (IllegalArgumentException | SQLException e) {
            LOG.log(Level.WARNING, "Auto-pivot failed, using original ResultSet", e);
            return null;
        }
    }

    
    
    
    /**
     * Renames columns like "A_price", "B_price" -> "A", "B" if:
     *  - all non-by columns share the same postfix
     *  - renames do not collide
     */
    private static ResultSet tryRemovePivotPostfix( ResultSet rs, String byCol) throws SQLException {

        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();

        Map<String, String> rename = new LinkedHashMap<>();
        String postfix = null;

        for (int i = 1; i <= cols; i++) {
            String name = md.getColumnLabel(i);

            // Skip by-column (e.g. dt)
            if (name.equals(byCol)) {
                continue;
            }

            int idx = name.lastIndexOf('_');
            if (idx <= 0 || idx == name.length() - 1) {
                return rs; // malformed or mixed → abort
            }

            String p = name.substring(idx + 1);
            if (postfix == null) {
                postfix = p;
            } else if (!postfix.equals(p)) {
                return rs; // different postfixes → abort
            }

            rename.put(name, name.substring(0, idx));
        }

        if (!rename.isEmpty()) {
            return DBHelper.renameColumns(rs, rename);
        }

        return rs;
    }


}
