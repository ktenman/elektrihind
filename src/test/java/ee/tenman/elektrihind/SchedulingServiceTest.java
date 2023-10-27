package ee.tenman.elektrihind;

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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

    @InjectMocks
    private SchedulingService schedulingService;

    @Mock
    private ElectricityPricesService electricityPricesService;

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
        List<ElectricityPrice> prices = createSamplePrices();
        when(electricityPricesService.fetchDailyPrices()).thenReturn(prices);
        schedulingService.setLatestPrices(new ArrayList<>());

        schedulingService.fetchAndSendPrices();

        verify(telegramService).sendToTelegram(anyString());
    }

    @Test
    void fetchAndSendPrices_withNoNewPrices_shouldNotSendMessage() {
        List<ElectricityPrice> prices = createSamplePrices();
        schedulingService.setLatestPrices(prices);
        when(electricityPricesService.fetchDailyPrices()).thenReturn(prices);

        schedulingService.fetchAndSendPrices();

        verify(telegramService, never()).sendToTelegram(anyString());
    }

    @Test
    void canSendMessageToday_withNoMessagesSent_shouldReturnTrue() {
        boolean result = schedulingService.canSendMessageToday();
        assertThat(result).isTrue();
    }

    @Test
    void canSendMessageToday_withDailyLimitReached_shouldReturnFalse() {
        for (int i = 0; i < SchedulingService.DAILY_MESSAGE_LIMIT; i++) {
            schedulingService.incrementMessageCountForToday();
        }

        boolean result = schedulingService.canSendMessageToday();
        assertThat(result).isFalse();
    }

    @Test
    void formatPricesForTelegram_givenPrices_shouldFormatCorrectly() {
        List<ElectricityPrice> prices = createSamplePrices();
        String result = schedulingService.formatPricesForTelegram(prices);

        assertThat(result).contains("The most expensive:")
                .contains("The cheapest:");
    }

    @Test
    void sendMessageAndIncrementCount_givenFormattedPrices_shouldSendMessageAndIncrementCount() {
        String samplePrices = "Sample Formatted Prices";
        schedulingService.sendMessageAndIncrementCount(samplePrices);

        verify(telegramService).sendToTelegram(samplePrices);
        LocalDate today = LocalDate.now(clock);
        assertThat(schedulingService.getMessageCountPerDay().getUnchecked(today)).isEqualTo(1);
    }

    @Test
    void isNewPricesAvailable_whenPricesAreDifferentFromLatest_shouldReturnTrue() {
        List<ElectricityPrice> newPrices = createSamplePrices();
        schedulingService.setLatestPrices(new ArrayList<>());
        boolean result = schedulingService.isNewPricesAvailable(newPrices);

        assertThat(result).isTrue();
    }

    @Test
    void isNewPricesAvailable_whenPricesAreSameAsLatest_shouldReturnFalse() {
        List<ElectricityPrice> samePrices = createSamplePrices();
        schedulingService.setLatestPrices(new ArrayList<>(samePrices));
        boolean result = schedulingService.isNewPricesAvailable(samePrices);

        assertThat(result).isFalse();
    }

    @Test
    void fetchAndSendPrices_whenCanSendMessageTodayIsTrue_shouldSendPrices() {
        List<ElectricityPrice> newPrices = createSamplePrices();
        when(electricityPricesService.fetchDailyPrices()).thenReturn(newPrices);
        schedulingService.setLatestPrices(new ArrayList<>());
        LocalDate today = LocalDate.now(clock);
        schedulingService.getMessageCountPerDay().put(today, 1);
        schedulingService.fetchAndSendPrices();

        verify(telegramService).sendToTelegram(anyString());
    }

    @Test
    void fetchAndSendPrices_whenCanSendMessageTodayIsFalse_shouldNotSendPrices() {
        List<ElectricityPrice> newPrices = createSamplePrices();
        when(electricityPricesService.fetchDailyPrices()).thenReturn(newPrices);
        schedulingService.setLatestPrices(new ArrayList<>());
        LocalDate today = LocalDate.now(clock);
        schedulingService.getMessageCountPerDay().put(today, SchedulingService.DAILY_MESSAGE_LIMIT);
        schedulingService.fetchAndSendPrices();

        verify(telegramService, never()).sendToTelegram(anyString());
    }

    private List<ElectricityPrice> createSamplePrices() {
        return List.of(
                new ElectricityPrice(Instant.parse("2023-10-27T11:00:00.00Z").atZone(ZoneId.systemDefault()).toLocalDateTime(), 100.0),
                new ElectricityPrice(Instant.parse("2023-10-27T12:00:00.00Z").atZone(ZoneId.systemDefault()).toLocalDateTime(), 50.0),
                new ElectricityPrice(Instant.parse("2023-10-28T09:00:00.00Z").atZone(ZoneId.systemDefault()).toLocalDateTime(), 150.0)
        );
    }
}
