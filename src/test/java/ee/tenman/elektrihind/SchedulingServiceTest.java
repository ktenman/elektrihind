package ee.tenman.elektrihind;

import ee.tenman.elektrihind.cache.CacheService;
import ee.tenman.elektrihind.electricity.ElectricityPrice;
import ee.tenman.elektrihind.electricity.ElectricityPricesService;
import ee.tenman.elektrihind.telegram.TelegramService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

    @InjectMocks
    private SchedulingService schedulingService;

    @Mock
    private ElectricityPricesService electricityPricesService;

    @Mock
    private CacheService cacheService;

    @Mock
    private TelegramService telegramService;

    @Mock
    private Clock clock;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2023-10-27T10:00:00.00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }

    @Test
    void fetchAndSendPrices_withNewPrices_shouldSendMessage() {
        mockFetchDailyPrices(createSamplePrices());
        when(cacheService.canSendMessageToday()).thenReturn(true);
        when(cacheService.getLatestPrices()).thenReturn(List.of());
        when(telegramService.formatPricesForTelegram(createSamplePrices())).thenReturn("Sample Formatted Prices");

        schedulingService.fetchAndSendPrices();

        verify(telegramService).sendToTelegram(anyString());
    }

    @Test
    void fetchAndSendPrices_withNoNewPrices_shouldNotSendMessage() {
        List<ElectricityPrice> prices = createSamplePrices();
        when(cacheService.getLatestPrices()).thenReturn(prices);
        mockFetchDailyPrices(prices);

        schedulingService.fetchAndSendPrices();

        verify(telegramService, never()).sendToTelegram(anyString());
    }

    @Test
    void sendMessageAndIncrementCount_givenFormattedPrices_shouldSendMessageAndIncrementCount() {
        String samplePrices = "Sample Formatted Prices";
        schedulingService.sendMessageAndIncrementCount(samplePrices);

        verify(telegramService).sendToTelegram(samplePrices);
        verify(cacheService, times(1)).incrementMessageCountForToday();
    }

    @Test
    void isNewPricesAvailable_whenPricesAreDifferentFromLatest_shouldReturnTrue() {
        when(cacheService.getLatestPrices()).thenReturn(List.of());

        assertThat(schedulingService.isNewPricesAvailable(createSamplePrices())).isTrue();
    }

    @Test
    void isNewPricesAvailable_whenPricesAreSameAsLatest_shouldReturnFalse() {
        List<ElectricityPrice> samePrices = createSamplePrices();
        when(cacheService.getLatestPrices()).thenReturn(samePrices);

        assertThat(schedulingService.isNewPricesAvailable(samePrices)).isFalse();
    }

    @Test
    void fetchAndSendPrices_whenCanSendMessageTodayIsTrue_shouldSendPrices() {
        mockFetchDailyPrices(createSamplePrices());
        when(cacheService.getLatestPrices()).thenReturn(List.of());
        when(cacheService.canSendMessageToday()).thenReturn(true);
        when(telegramService.formatPricesForTelegram(createSamplePrices())).thenReturn("Sample Formatted Prices");

        schedulingService.fetchAndSendPrices();

        verify(telegramService).sendToTelegram(anyString());
    }

    @Test
    void fetchAndSendPrices_whenCanSendMessageTodayIsFalse_shouldNotSendPrices() {
        mockFetchDailyPrices(createSamplePrices());
        when(cacheService.getLatestPrices()).thenReturn(List.of());
        when(cacheService.canSendMessageToday()).thenReturn(false);

        schedulingService.fetchAndSendPrices();

        verify(telegramService, never()).sendToTelegram(anyString());
    }

    private List<ElectricityPrice> createSamplePrices() {
        return List.of(
                newPrice("2023-10-27T11:00:00.00Z", 100.0),
                newPrice("2023-10-27T12:00:00.00Z", 50.0),
                newPrice("2023-10-28T09:00:00.00Z", 150.0)
        );
    }

    private ElectricityPrice newPrice(String instant, double price) {
        return new ElectricityPrice(Instant.parse(instant).atZone(ZoneId.systemDefault()).toLocalDateTime(), price);
    }

    private void mockFetchDailyPrices(List<ElectricityPrice> prices) {
        when(electricityPricesService.fetchDailyPrices()).thenReturn(prices);
    }

}
