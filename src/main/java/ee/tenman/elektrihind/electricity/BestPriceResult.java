package ee.tenman.elektrihind.electricity;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class BestPriceResult {
    private final String startTime;
    private final Double totalCost;
    private final Double averagePrice;

    public BestPriceResult(String startTime, Double totalCost, int durationInMinutes) {
        this.startTime = startTime;
        this.totalCost = BigDecimal.valueOf(totalCost)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        this.averagePrice = BigDecimal.valueOf(this.totalCost / (durationInMinutes / 60.0))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
