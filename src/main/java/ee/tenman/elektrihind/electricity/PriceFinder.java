package ee.tenman.elektrihind.electricity;

import java.time.LocalDateTime;
import java.util.List;

public class PriceFinder {

    private static final int MINUTES_IN_HOUR = 60;

    public static BestPriceResult findBestPriceForDuration(List<ElectricityPrice> electricityPrices, int durationInMinutes) {
        if (electricityPrices == null || electricityPrices.isEmpty() || durationInMinutes <= 0) {
            throw new IllegalArgumentException("Invalid input");
        }

        BestPriceResult bestPriceResult = null;
        double lowestTotalCost = Double.MAX_VALUE;
        LocalDateTime bestStartTime = null;

        // Loop through every minute as a potential start time
        for (int startMinute = 0; startMinute < electricityPrices.size() * MINUTES_IN_HOUR; startMinute++) {
            double totalCost = 0.0;
            int minutesCounted = 0;
            int index = startMinute / MINUTES_IN_HOUR; // Find the index in the prices list
            int minuteOfHour = startMinute % MINUTES_IN_HOUR; // Find the minute within the hour

            while (minutesCounted < durationInMinutes && index < electricityPrices.size()) {
                double hourlyPrice = electricityPrices.get(index).getPrice();
                // Calculate the cost for the remaining part of the current hour or the remaining duration
                int minutesToCalculate = Math.min(MINUTES_IN_HOUR - minuteOfHour, durationInMinutes - minutesCounted);
                totalCost += (hourlyPrice / MINUTES_IN_HOUR) * minutesToCalculate;
                minutesCounted += minutesToCalculate;

                // Move to the next hour
                index++;
                minuteOfHour = 0; // Reset minute of hour after moving to next hour
            }

            if (minutesCounted == durationInMinutes && totalCost < lowestTotalCost) {
                lowestTotalCost = totalCost;
                bestStartTime = electricityPrices.get(startMinute / MINUTES_IN_HOUR).getDate().plusMinutes(startMinute % MINUTES_IN_HOUR);
                bestPriceResult = new BestPriceResult(bestStartTime.toString(), lowestTotalCost, durationInMinutes);
            }
        }

        return bestPriceResult;
    }
}
