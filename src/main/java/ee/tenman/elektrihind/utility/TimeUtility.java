package ee.tenman.elektrihind.utility;

import ee.tenman.elektrihind.electricity.ElectricityPrice;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class TimeUtility {

    private static final int MINUTES_IN_HOUR = 60;

    public static LocalDateTime getStartTime(List<ElectricityPrice> electricityPrices, int startMinute) {
        if (electricityPrices == null || electricityPrices.isEmpty()) {
            throw new IllegalArgumentException("Electricity prices list cannot be null or empty.");
        }
        int startHourIndex = startMinute / MINUTES_IN_HOUR;
        int minuteOffset = startMinute % MINUTES_IN_HOUR;
        if (startHourIndex >= electricityPrices.size()) {
            throw new IllegalArgumentException("Start hour index out of bounds.");
        }
        return electricityPrices.get(startHourIndex).getDate().plusMinutes(minuteOffset);
    }

    public static CustomDuration durationInSeconds(AtomicLong startTime) {
        return new CustomDuration(startTime.get());
    }

    public static CustomDuration durationInSeconds(long startTime) {
        return new CustomDuration(startTime);
    }

    private static String formatDuration(double duration) {
        return String.format("%.3f", duration);
    }

    public static class CustomDuration {
        private final double durationInSeconds;

        public CustomDuration(long startTime) {
            this.durationInSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        }

        public String asString() {
            return TimeUtility.formatDuration(durationInSeconds);
        }

        public double asDouble() {
            return durationInSeconds;
        }

        @Override
        public String toString() {
            return asString();
        }
    }
}
