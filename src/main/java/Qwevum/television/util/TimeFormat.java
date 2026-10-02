package Qwevum.television.util;

import java.util.concurrent.TimeUnit;

public final class TimeFormat {
    private TimeFormat() {}

    /** Formats a duration in milliseconds as HH:MM:SS. */
    public static String format(long timeInMillis) {
        final long hours = TimeUnit.MILLISECONDS.toHours(timeInMillis);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60;
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
