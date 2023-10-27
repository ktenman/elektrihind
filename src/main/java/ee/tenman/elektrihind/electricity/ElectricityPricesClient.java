package ee.tenman.elektrihind.electricity;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "electricityPricesClient", url = "https://elektrihind.ee/api")
public interface ElectricityPricesClient {

    @GetMapping("/stock_price_daily.php")
    List<ElectricityPrice> fetchDailyPrices();
}
