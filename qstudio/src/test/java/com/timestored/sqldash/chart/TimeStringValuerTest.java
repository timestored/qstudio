package com.timestored.sqldash.chart;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;

import org.junit.Test;

public class TimeStringValuerTest {

	private final TimeStringValuer valuer = new TimeStringValuer();

	@Test public void timestampWithWholeMillisecondsHasThreeFractionDigits() {
		assertEquals("2026-10-12T00:00:19.950", valuer.format(Timestamp.valueOf("2026-10-12 00:00:19.95")));
		assertEquals("2026-10-12T00:00:19.000", valuer.format(Timestamp.valueOf("2026-10-12 00:00:19")));
	}

	@Test public void timestampWithMicrosecondsHasSixFractionDigits() {
		assertEquals("2026-10-12T00:00:19.950123", valuer.format(Timestamp.valueOf("2026-10-12 00:00:19.950123")));
		assertEquals("2026-10-12T00:00:19.950001", valuer.format(Timestamp.valueOf("2026-10-12 00:00:19.950001")));
		assertEquals("2026-10-12T00:00:19.950100", valuer.format(Timestamp.valueOf("2026-10-12 00:00:19.9501")));
	}
}
