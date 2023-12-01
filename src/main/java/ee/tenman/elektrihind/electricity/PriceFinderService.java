package ee.tenman.elektrihind.electricity;

import ee.tenman.elektrihind.utility.TimeUtility;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PriceFinderService {

    @Resource
    private Clock clock;

    private static final int MINUTES_IN_HOUR = 60;

    private void validateInput(List<ElectricityPrice> electricityPrices, int durationInMinutes) {
        if (electricityPrices == null || electricityPrices.isEmpty()) {
            throw new IllegalArgumentException("Electricity prices list cannot be null or empty.");
        }
        if (durationInMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be greater than 0.");
        }
    }

    public BestPriceResult findBestPriceForDuration(List<ElectricityPrice> electricityPrices, int durationInMinutes) {
        validateInput(electricityPrices, durationInMinutes);

        BestPriceResult bestPriceResult = null;
        double lowestTotalCost = Double.MAX_VALUE;
        LocalDateTime now = LocalDateTime.now(clock);

        for (int startMinuteIndex = 0; startMinuteIndex <= electricityPrices.size() * MINUTES_IN_HOUR - durationInMinutes; startMinuteIndex++) {
            LocalDateTime currentStartTime = TimeUtility.getStartTime(electricityPrices, startMinuteIndex);

            if (currentStartTime.isBefore(now)) {
                continue;
            }

            double currentIntervalCost = calculateCostForInterval(electricityPrices, startMinuteIndex, durationInMinutes);

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

    private double calculateCostForInterval(List<ElectricityPrice> electricityPrices, int startMinute, int durationInMinutes) {
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


    public BigDecimal calculateImmediateCost(List<ElectricityPrice> prices, int durationInMinutes) {
        LocalDateTime startTime = LocalDateTime.now(clock);
        LocalDateTime endTime = startTime.plusMinutes(durationInMinutes);
        BigDecimal totalCost = BigDecimal.ZERO;

        for (int i = 0; i < prices.size(); i++) {
            ElectricityPrice currentPrice = prices.get(i);
            LocalDateTime nextPriceTime = (i < prices.size() - 1) ? prices.get(i + 1).getDate() : endTime;

            if (isPriceApplicable(currentPrice, startTime, nextPriceTime)) {
                LocalDateTime intervalEnd = nextPriceTime.isBefore(endTime) ? nextPriceTime : endTime;
                BigDecimal calculatedCostForInterval = calculateCostForInterval(currentPrice, startTime, intervalEnd);
                totalCost = totalCost.add(calculatedCostForInterval);

                startTime = intervalEnd;
                if (startTime.isEqual(endTime)) {
                    break;
                }
            }
        }

        return totalCost.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isPriceApplicable(ElectricityPrice price, LocalDateTime startTime, LocalDateTime nextPriceTime) {
        return !price.getDate().isAfter(startTime) && nextPriceTime.isAfter(startTime);
    }

    private BigDecimal calculateCostForInterval(ElectricityPrice price, LocalDateTime startTime, LocalDateTime intervalEnd) {
        LocalDateTime effectiveStartTime = startTime.isBefore(price.getDate()) ? price.getDate() : startTime;
        long secondsAtPrice = Duration.between(effectiveStartTime, intervalEnd).toSeconds();
        BigDecimal hourlyRate = BigDecimal.valueOf(price.getPrice()).divide(BigDecimal.valueOf(3600), 10, RoundingMode.HALF_UP);
        return hourlyRate.multiply(new BigDecimal(secondsAtPrice));
    }

    public Optional<ElectricityPrice> currentPrice(List<ElectricityPrice> electricityPrices) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime key = LocalDateTime.of(now.toLocalDate(),
                now.toLocalTime().withHour(now.getHour()).withMinute(0).withSecond(0).withNano(0));
        return electricityPrices.stream().filter(d -> d.getDate().equals(key))
                .findFirst();
    }
}
