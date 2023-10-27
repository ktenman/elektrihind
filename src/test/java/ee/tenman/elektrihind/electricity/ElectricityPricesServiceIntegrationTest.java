package ee.tenman.elektrihind.electricity;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWireMock(port = 0)
@Slf4j
class ElectricityPricesServiceIntegrationTest {

    @Resource
    private ElectricityPricesService electricityPricesService;

    private static final String ENDPOINT_DAILY_PRICES = "/stock_price_daily.php";
    private static final String RESPONSE_FILE_DAILY_PRICES = "__files/daily_prices_response.json";

    @Test
    void testFetchDailyPrices() {
        stubFor(get(urlEqualTo(ENDPOINT_DAILY_PRICES)).willReturn(aResponse()
                .withBodyFile(RESPONSE_FILE_DAILY_PRICES)));

        List<ElectricityPrice> prices = electricityPricesService.fetchDailyPrices();

        assertThat(prices)
                .isNotNull()
                .hasSize(73)
                .first()
                .returns(LocalDateTime.parse("2023-10-26T00:00"), ElectricityPrice::getDate)
                .returns(13.54, ElectricityPrice::getPrice);
    }
}
