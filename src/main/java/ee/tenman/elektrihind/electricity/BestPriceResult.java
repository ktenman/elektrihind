package ee.tenman.elektrihind.electricity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BestPriceResult {
    private LocalDateTime startTime;
    private Double totalCost;
    private Double averagePrice;

    public BestPriceResult(LocalDateTime startTime, Double totalCost, int durationInMinutes) {
        this.startTime = startTime;
        this.totalCost = BigDecimal.valueOf(totalCost)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        this.averagePrice = BigDecimal.valueOf(this.totalCost / (durationInMinutes / 60.0))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
