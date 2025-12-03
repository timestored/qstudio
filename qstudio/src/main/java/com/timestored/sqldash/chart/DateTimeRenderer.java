package com.timestored.sqldash.chart;

import java.awt.Component;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class DateTimeRenderer extends DefaultTableCellRenderer {
	
	public static enum DateTimeFormatType {
	    DATE_YYYYMMDD,     // _SD_DATE
	    DATE_MMDDYYYY,     // _SD_DATEMM
	    DATE_DDMMYYYY,     // _SD_DATEDD
	    DATE_MONTH_LONG,   // _SD_DATEMONTH
	    DATE_MONTH_SHORT,  // _SD_DATEMON
	    TIME_HHMMSS_MS,    // _SD_TIME
	    TIME_HHMMSS,       // _SD_TIMESS
	    TIME_HHMM
	}
	
	private static final Map<DateTimeFormatType, DateTimeFormatter> FMT = Map.of(
		    DateTimeFormatType.DATE_YYYYMMDD,  DateTimeFormatter.ofPattern("yyyy-MM-dd"),
		    DateTimeFormatType.DATE_MMDDYYYY,  DateTimeFormatter.ofPattern("MM/dd/yyyy"),
		    DateTimeFormatType.DATE_DDMMYYYY,  DateTimeFormatter.ofPattern("dd/MM/yyyy"),
		    DateTimeFormatType.DATE_MONTH_LONG,DateTimeFormatter.ofPattern("d MMMM yyyy"),
		    DateTimeFormatType.DATE_MONTH_SHORT,DateTimeFormatter.ofPattern("d MMM yy"),
		    DateTimeFormatType.TIME_HHMMSS_MS, DateTimeFormatter.ofPattern("HH:mm:ss.SSS"),
		    DateTimeFormatType.TIME_HHMMSS,    DateTimeFormatter.ofPattern("HH:mm:ss"),
		    DateTimeFormatType.TIME_HHMM,      DateTimeFormatter.ofPattern("HH:mm")
		);

	private static DateTimeFormatType detectDateTimeType(String name) {
	    name = name.toUpperCase();

	    if (name.endsWith("_SD_DATE"))       return DateTimeFormatType.DATE_YYYYMMDD;
	    if (name.endsWith("_SD_DATEMM"))     return DateTimeFormatType.DATE_MMDDYYYY;
	    if (name.endsWith("_SD_DATEDD"))     return DateTimeFormatType.DATE_DDMMYYYY;
	    if (name.endsWith("_SD_DATEMONTH"))  return DateTimeFormatType.DATE_MONTH_LONG;
	    if (name.endsWith("_SD_DATEMON"))    return DateTimeFormatType.DATE_MONTH_SHORT;

	    if (name.endsWith("_SD_TIME"))       return DateTimeFormatType.TIME_HHMMSS_MS;
	    if (name.endsWith("_SD_TIMESS"))     return DateTimeFormatType.TIME_HHMMSS;
	    if (name.endsWith("_SD_TIMEMM"))     return DateTimeFormatType.TIME_HHMM;

	    return null;
	}

	public static DateTimeRenderer forColumnName(String colName) {
	    DateTimeFormatType type = detectDateTimeType(colName);
	    if (type != null) {
	        return new DateTimeRenderer(type);
	    }
	    return null;
	}
	
    private final DateTimeFormatType type;
    private final DateTimeFormatter formatter;

    public DateTimeRenderer(DateTimeFormatType type) {
        this.type = type;
        this.formatter = FMT.get(type);
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int col) {

        JLabel lbl = (JLabel)super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,col);

        if (value == null) {
            lbl.setText("");
            return lbl;
        }

        LocalDateTime ldt = toLocalDateTime(value);
        if (ldt == null) {
            lbl.setText(value.toString());
            return lbl;
        }

        // date-only formats → strip time
        if (type.name().startsWith("DATE_")) {
            lbl.setText(formatter.format(ldt.toLocalDate()));
        }
        // time-only formats → strip date
        else if (type.name().startsWith("TIME_")) {
            lbl.setText(formatter.format(ldt.toLocalTime()));
        }

        return lbl;
    }

    private LocalDateTime toLocalDateTime(Object v) {
        if (v instanceof LocalDateTime) return (LocalDateTime) v;
        if (v instanceof LocalDate) return ((LocalDate) v).atStartOfDay();
        if (v instanceof LocalTime) return LocalDate.now().atTime((LocalTime) v);
        if (v instanceof ZonedDateTime) return ((ZonedDateTime)v).toLocalDateTime();
        if (v instanceof Instant) return LocalDateTime.ofInstant((Instant)v, ZoneId.systemDefault());
        if (v instanceof java.util.Date) return Instant.ofEpochMilli(((java.util.Date)v).getTime())
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
        return null;
    }
}

