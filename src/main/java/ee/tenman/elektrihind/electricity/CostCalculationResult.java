package ee.tenman.elektrihind.electricity;

import java.math.BigDecimal;

public record CostCalculationResult(
        BigDecimal totalKwh,
        BigDecimal totalCost,
        BigDecimal totalDayKwh,
        BigDecimal totalNightKwh
) {
}
