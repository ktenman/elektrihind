package ee.tenman.elektrihind.utility;

import ee.tenman.elektrihind.electricity.ElectricityPrice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class TimeUtility {

    private static final int MINUTES_IN_HOUR = 60;

    public static LocalDateTime getStartTime(List<ElectricityPrice> electricityPrices, int startMinute) {
        int startHourIndex = startMinute / MINUTES_IN_HOUR;
        int minuteOffset = startMinute % MINUTES_IN_HOUR;
        return electricityPrices.get(startHourIndex).getDate().plusMinutes(minuteOffset);
    }

    public static Duration durationInSeconds(AtomicLong startTime) {
        long endTime = System.nanoTime();
        return new Duration((endTime - startTime.get()) / 1_000_000_000.0);
    }

    public static Duration durationInSeconds(long startTime) {
        long endTime = System.nanoTime();
        return new Duration((endTime - startTime) / 1_000_000_000.0);
    }

    private static String formatDuration(double duration) {
        return String.format("%.3f", duration);
    }

    public static class Duration {
        private final double durationInSeconds;

        public Duration(double durationInSeconds) {
            this.durationInSeconds = durationInSeconds;
        }

        public String asString() {
            return TimeUtility.formatDuration(durationInSeconds);
        }

        public double asDouble() {
            return durationInSeconds;
        }
    }
}
