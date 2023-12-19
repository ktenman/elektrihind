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

    public static String durationInSeconds(AtomicLong startTime) {
        long endTime = System.nanoTime();
        double duration = (endTime - startTime.get()) / 1_000_000_000.0;
        return String.format("%.3f", duration);
    }

    public static String durationInSeconds(long startTime) {
        long endTime = System.nanoTime();
        double duration = (endTime - startTime) / 1_000_000_000.0;
        return String.format("%.3f", duration);
    }
}
