package ee.tenman.elektrihind.car.automaks;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class TaxResponse {
    private BigDecimal registrationTax;
    private BigDecimal annualTax;

    public BigDecimal getRegistrationTax() {
        return registrationTax != null ? registrationTax.setScale(2, RoundingMode.HALF_UP) : null;
    }

    public BigDecimal getAnnualTax() {
        return annualTax != null ? annualTax.setScale(2, RoundingMode.HALF_UP) : null;
    }
}
