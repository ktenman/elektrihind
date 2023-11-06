package ee.tenman.elektrihind;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.math.BigDecimal;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@EnableRetry
public class ElektrihindApplication {
    public static void main(String[] args) {
        SpringApplication.run(ElektrihindApplication.class, args);
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Getter
    public static class CostCalculationResult {
        private BigDecimal totalKwh;
        private BigDecimal totalCost;
        private BigDecimal totalDayKwh;
        private BigDecimal totalNightKwh;

    }
}
