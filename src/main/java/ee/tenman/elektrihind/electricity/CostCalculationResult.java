package ee.tenman.elektrihind.electricity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class CostCalculationResult {
    private BigDecimal totalKwh;
    private BigDecimal totalCost;
    private BigDecimal totalDayKwh;
    private BigDecimal totalNightKwh;

}
