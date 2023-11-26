package ee.tenman.elektrihind.utility;

import ee.tenman.elektrihind.electricity.ElectricityPrice;

import java.time.LocalDateTime;
import java.util.List;

public class TimeUtility {

    private static final int MINUTES_IN_HOUR = 60;

    public static LocalDateTime getStartTime(List<ElectricityPrice> electricityPrices, int startMinute) {
        int startHourIndex = startMinute / MINUTES_IN_HOUR;
        int minuteOffset = startMinute % MINUTES_IN_HOUR;
        return electricityPrices.get(startHourIndex).getDate().plusMinutes(minuteOffset);
    }
}
