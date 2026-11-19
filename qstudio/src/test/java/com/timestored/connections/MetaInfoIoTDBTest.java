package com.timestored.connections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.timestored.connections.MetaInfo.ColumnInfo;

public class MetaInfoIoTDBTest {

	@Test
	public void mapsTreeTimeseriesToDeviceTableAndMeasurementColumn() {
		ColumnInfo column = MetaInfo.fromIoTDBTreeSeries("root.sg.device.temperature", "FLOAT");

		assertEquals("root.sg.device", column.getTableName());
		assertEquals("temperature", column.getColumnName());
		assertEquals("root.sg.device", column.getFullTableName());
		assertEquals("FLOAT", column.getTypeName());
	}

	@Test
	public void keepsDotsInsideQuotedTreePathNodes() {
		ColumnInfo column = MetaInfo.fromIoTDBTreeSeries("root.sg.`device.with.dot`.temperature", "FLOAT");

		assertEquals("root.sg.`device.with.dot`", column.getTableName());
		assertEquals("temperature", column.getColumnName());
	}

	@Test
	public void mapsTableModelColumnsToDatabaseAndTable() {
		ColumnInfo column = MetaInfo.fromIoTDBTableColumn("telemetry", "readings", "device_id", "STRING");

		assertEquals("telemetry", column.getNamespace());
		assertEquals("readings", column.getTableName());
		assertEquals("telemetry.readings", column.getFullTableName());
		assertEquals("device_id", column.getColumnName());
		assertEquals("STRING", column.getTypeName());
	}

	@Test
	public void ignoresInvalidTreeSeriesPaths() {
		assertNull(MetaInfo.fromIoTDBTreeSeries("root", "FLOAT"));
		assertNull(MetaInfo.fromIoTDBTreeSeries("root.sg.", "FLOAT"));
	}
}
