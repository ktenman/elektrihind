package ee.tenman.elektrihind.electricity;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class PriceFinder {

    private static final int MINUTES_IN_HOUR = 60;

    private static void validateInput(List<ElectricityPrice> electricityPrices, int durationInMinutes) {
        if (electricityPrices == null || electricityPrices.isEmpty()) {
            throw new IllegalArgumentException("Electricity prices list cannot be null or empty.");
        }
        if (durationInMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be greater than 0.");
        }
    }

    public static BestPriceResult findBestPriceForDuration(List<ElectricityPrice> electricityPrices, int durationInMinutes) {
        validateInput(electricityPrices, durationInMinutes);

        BestPriceResult bestPriceResult = null;
        double lowestTotalCost = Double.MAX_VALUE;

        for (int startMinuteIndex = 0; startMinuteIndex <= electricityPrices.size() * MINUTES_IN_HOUR - durationInMinutes; startMinuteIndex++) {
            double currentIntervalCost = calculateCostForInterval(electricityPrices, startMinuteIndex, durationInMinutes);
            LocalDateTime currentStartTime = getStartTime(electricityPrices, startMinuteIndex);

            boolean isNewLowestCost = currentIntervalCost < lowestTotalCost;
            boolean isSameCostButEarlierStart = currentIntervalCost == lowestTotalCost &&
                    (bestPriceResult == null || currentStartTime.isBefore(bestPriceResult.getStartTime()));

            if (isNewLowestCost || isSameCostButEarlierStart) {
                lowestTotalCost = currentIntervalCost;
                bestPriceResult = new BestPriceResult(currentStartTime, lowestTotalCost, durationInMinutes);
            }
        }

        return bestPriceResult;
    }

    private static double calculateCostForInterval(List<ElectricityPrice> electricityPrices, int startMinute, int durationInMinutes) {
        double totalCost = 0.0;
        int currentMinute = startMinute;
        while (durationInMinutes > 0) {
            int hourIndex = currentMinute / MINUTES_IN_HOUR;
            int minuteOfHour = currentMinute % MINUTES_IN_HOUR;
            ElectricityPrice currentPrice = electricityPrices.get(hourIndex);

            int minutesInCurrentHour = Math.min(MINUTES_IN_HOUR - minuteOfHour, durationInMinutes);
            totalCost += (currentPrice.getPrice() / MINUTES_IN_HOUR) * minutesInCurrentHour;

            durationInMinutes -= minutesInCurrentHour;
            currentMinute += minutesInCurrentHour;
        }
        return totalCost;
    }

    private static LocalDateTime getStartTime(List<ElectricityPrice> electricityPrices, int startMinute) {
        int startHourIndex = startMinute / MINUTES_IN_HOUR;
        int minuteOffset = startMinute % MINUTES_IN_HOUR;
        return electricityPrices.get(startHourIndex).getDate().plusMinutes(minuteOffset);
    }

}
