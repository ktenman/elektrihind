package ee.tenman.elektrihind;

import ee.tenman.elektrihind.electricity.ElekterBotService;
import ee.tenman.elektrihind.telegram.TelegramService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import wiremock.com.google.common.net.HttpHeaders;
import wiremock.com.google.common.net.MediaType;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.mockito.Mockito.lenient;

@IntegrationTest
class SchedulingServiceIntegrationTest {

    private static final String ENDPOINT_DAILY_PRICES = "/stock_price_daily.php";
    private static final String RESPONSE_FILE_DAILY_PRICES = "daily_prices_response.json";
    @MockBean
    Clock clock;
    @Resource
    CacheService cacheService;
    @MockBean
    ElekterBotService elekterBotService;
    @MockBean
    TelegramService telegramService;
    @TempDir
    Path tempDir;
    @Resource
    private SchedulingService schedulingService;

    @BeforeEach
    void setUp() {
        String tempCacheFilePath = tempDir.resolve("cache_file.dat").toString();
        ReflectionTestUtils.setField(cacheService, "cacheFilePath", tempCacheFilePath);
        cacheService.init();
        lenient().when(clock.instant()).thenReturn(Instant.parse("2023-10-27T10:00:00.00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }

    @Test
    void testFetchDailyPrices() {
        stubFor(get(urlEqualTo(ENDPOINT_DAILY_PRICES)).willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
                .withBodyFile(RESPONSE_FILE_DAILY_PRICES)));

        schedulingService.fetchAndSendPrices();
    }


}
