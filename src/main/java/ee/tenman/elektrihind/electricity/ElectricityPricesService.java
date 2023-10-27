package ee.tenman.elektrihind.electricity;

import feign.FeignException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ElectricityPricesService {

    @Resource
    private ElectricityPricesClient electricityPricesClient;

    @Retryable(value = FeignException.class, maxAttempts = 6, backoff = @Backoff(delay = 1500))
    public List<ElectricityPrice> fetchDailyPrices() {
        log.info("Fetching daily prices");
        List<ElectricityPrice> electricityPrices = electricityPricesClient.fetchDailyPrices();
        log.info("Fetched daily prices: {}", electricityPrices);
        return electricityPrices;
    }

}
