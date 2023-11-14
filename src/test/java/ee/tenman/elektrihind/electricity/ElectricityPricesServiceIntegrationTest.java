package ee.tenman.elektrihind.electricity;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import wiremock.com.google.common.net.HttpHeaders;
import wiremock.com.google.common.net.MediaType;

import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ElectricityPricesServiceIntegrationTest {

    @Resource
    private ElectricityPricesService electricityPricesService;

    private static final String ENDPOINT_DAILY_PRICES = "/stock_price_daily.php";
    private static final String RESPONSE_FILE_DAILY_PRICES = "daily_prices_response.json";

    @Test
    void testFetchDailyPrices() {
        stubFor(get(urlEqualTo(ENDPOINT_DAILY_PRICES)).willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
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
