package Qwevum.television.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeFormatTest {

    @Test
    void formatsShortTracks() {
        assertEquals("00:03:25", TimeFormat.format(205_000));
    }

    @Test
    void wrapsMinutesAfterAnHour() {
        // 1h 5m 7s used to show as 01:65:07
        assertEquals("01:05:07", TimeFormat.format(3_907_000));
    }

    @Test
    void humanFormatSkipsEmptyUnits() {
        assertEquals("45s", TimeFormat.human(45_000));
        assertEquals("3m 3s", TimeFormat.human(183_000));
        assertEquals("1h 5m 7s", TimeFormat.human(3_907_000));
    }
}
