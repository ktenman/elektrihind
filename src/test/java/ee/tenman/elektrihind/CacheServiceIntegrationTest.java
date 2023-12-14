package ee.tenman.elektrihind;

import ee.tenman.elektrihind.cache.CacheService;
import ee.tenman.elektrihind.electricity.ElectricityBotService;
import ee.tenman.elektrihind.electricity.ElectricityPricesService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@IntegrationTest
class CacheServiceIntegrationTest {

    @MockBean
    ElectricityBotService elekterBotService;
    @MockBean
    ElectricityPricesService electricityPricesService;
    @MockBean
    Clock clock;
    @Resource
    CacheService cacheService;

    @Resource
    private CacheManager cacheManager;

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

    @Test
    void isAutomaticCachingEnabled() {
        cacheService.setAutomaticFetchingEnabled(false);

        assertThat(cacheService.isAutomaticFetchingEnabled()).isFalse();

        cacheService.setAutomaticFetchingEnabled(true);

        assertThat(cacheService.isAutomaticFetchingEnabled()).isTrue();
    }


}
