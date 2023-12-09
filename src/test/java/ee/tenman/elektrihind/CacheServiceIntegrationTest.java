package ee.tenman.elektrihind;

import ee.tenman.elektrihind.cache.CacheService;
import ee.tenman.elektrihind.electricity.ElectricityPricesService;
import ee.tenman.elektrihind.electricity.ElekterBotService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@IntegrationTest
class CacheServiceIntegrationTest {

    @MockBean
    ElekterBotService elekterBotService;
    @MockBean
    ElectricityPricesService electricityPricesService;
    @MockBean
    Clock clock;
    @Resource
    CacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService.clearCache();
        lenient().when(clock.instant()).thenReturn(Instant.parse("2023-10-27T10:00:00.00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }

    @Test
    void testCanSendMessageToday() {
        assertThat(cacheService.canSendMessageToday()).isTrue();

        cacheService.incrementMessageCountForToday();

        assertThat(cacheService.canSendMessageToday()).isFalse();
    }

}
