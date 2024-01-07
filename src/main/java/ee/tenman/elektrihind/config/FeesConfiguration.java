package ee.tenman.elektrihind.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
@Getter
public class FeesConfiguration {

    private final ResourceLoader resourceLoader;

    private BigDecimal fixedSurcharge;
    private BigDecimal monthlyFee;
    private BigDecimal dayDistributionFee;
    private BigDecimal nightDistributionFee;
    private BigDecimal apartmentMonthlyFee;
    private BigDecimal renewableEnergyFee;
    private BigDecimal electricityExciseTax;
    private BigDecimal salesTax;
    @Resource
    ObjectMapper objectMapper;

    public FeesConfiguration(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        try {
            Map<String, String> feesMap = objectMapper.readValue(
                    resourceLoader.getResource("classpath:fees.json").getInputStream(),
                    new TypeReference<>() {
                    });

            fixedSurcharge = new BigDecimal(feesMap.get("fixedSurcharge"));
            monthlyFee = new BigDecimal(feesMap.get("monthlyFee"));
            dayDistributionFee = new BigDecimal(feesMap.get("dayDistributionFee"));
            nightDistributionFee = new BigDecimal(feesMap.get("nightDistributionFee"));
            apartmentMonthlyFee = new BigDecimal(feesMap.get("apartmentMonthlyFee"));
            renewableEnergyFee = new BigDecimal(feesMap.get("renewableEnergyFee"));
            electricityExciseTax = new BigDecimal(feesMap.get("electricityExciseTax"));
            salesTax = new BigDecimal(feesMap.get("salesTax"));
        } catch (IOException e) {
            log.error("Failed to read fees.json", e);
        }
    }
}
