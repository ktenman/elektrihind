package ee.tenman.elektrihind.electricity;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class BestPriceResult {
    private String startTime;
    private Double totalCost;
    private Double averagePrice; // New field for average price

    public BestPriceResult(String startTime, Double totalCost, int durationInMinutes) {
        this.startTime = startTime;
        this.totalCost = BigDecimal.valueOf(totalCost)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        // Calculate the average price per minute
        this.averagePrice = BigDecimal.valueOf(this.totalCost / (durationInMinutes / 60.0))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
