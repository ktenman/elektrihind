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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        mockFetchDailyPrices(createSamplePrices());
        schedulingService.setLatestPrices(new ArrayList<>());

        schedulingService.fetchAndSendPrices();

        verify(telegramService).sendToTelegram(anyString());
    }

    @Test
    void fetchAndSendPrices_withNoNewPrices_shouldNotSendMessage() {
        List<ElectricityPrice> prices = createSamplePrices();
        schedulingService.setLatestPrices(prices);
        mockFetchDailyPrices(prices);

        schedulingService.fetchAndSendPrices();

        verify(telegramService, never()).sendToTelegram(anyString());
    }

    @Test
    void canSendMessageToday_withNoMessagesSent_shouldReturnTrue() {
        assertThat(schedulingService.canSendMessageToday()).isTrue();
    }

    @Test
    void canSendMessageToday_withDailyLimitReached_shouldReturnFalse() {
        maxOutDailyMessageLimit();

        assertThat(schedulingService.canSendMessageToday()).isFalse();
    }

    @Test
    void formatPricesForTelegram_givenPrices_shouldFormatCorrectly() {
        String result = schedulingService.formatPricesForTelegram(createSamplePrices());

        assertThat(result).contains("Tomorrow, the most expensive:", "Today, the cheapest:");
    }

    @Test
    void sendMessageAndIncrementCount_givenFormattedPrices_shouldSendMessageAndIncrementCount() {
        String samplePrices = "Sample Formatted Prices";
        schedulingService.sendMessageAndIncrementCount(samplePrices);

        verify(telegramService).sendToTelegram(samplePrices);
        LocalDate today = LocalDate.now(clock);
        assertThat(schedulingService.getMessageCountPerDay().getIfPresent(today)).isEqualTo(1);
    }

    @Test
    void isNewPricesAvailable_whenPricesAreDifferentFromLatest_shouldReturnTrue() {
        schedulingService.setLatestPrices(new ArrayList<>());

        assertThat(schedulingService.isNewPricesAvailable(createSamplePrices())).isTrue();
    }

    @Test
    void isNewPricesAvailable_whenPricesAreSameAsLatest_shouldReturnFalse() {
        List<ElectricityPrice> samePrices = createSamplePrices();
        schedulingService.setLatestPrices(new ArrayList<>(samePrices));

        assertThat(schedulingService.isNewPricesAvailable(samePrices)).isFalse();
    }

    @Test
    void fetchAndSendPrices_whenCanSendMessageTodayIsTrue_shouldSendPrices() {
        mockFetchDailyPrices(createSamplePrices());
        schedulingService.setLatestPrices(new ArrayList<>());
        LocalDate today = LocalDate.now(clock);
        schedulingService.getMessageCountPerDay().put(today, 1);

        schedulingService.fetchAndSendPrices();

        verify(telegramService).sendToTelegram(anyString());
    }

    @Test
    void fetchAndSendPrices_whenCanSendMessageTodayIsFalse_shouldNotSendPrices() {
        mockFetchDailyPrices(createSamplePrices());
        schedulingService.setLatestPrices(new ArrayList<>());
        maxOutDailyMessageLimit();

        schedulingService.fetchAndSendPrices();

        verify(telegramService, never()).sendToTelegram(anyString());
    }

    @Test
    void getMessageCount_whenDateNotInCache_shouldReturnZero() {
        LocalDate someDate = LocalDate.of(2022, 1, 1);

        assertThat(schedulingService.getMessageCount(someDate)).isZero();
    }

    @Test
    void getMessageCount_whenDateInCache_shouldReturnCount() {
        LocalDate someDate = LocalDate.of(2022, 1, 1);
        schedulingService.getMessageCountPerDay().put(someDate, 5);

        assertThat(schedulingService.getMessageCount(someDate)).isEqualTo(5);
    }

    @Test
    void priceDateLabel_givenToday_shouldReturnTodayWithComma() {
        Instant now = Instant.parse("2023-10-27T10:00:00.00Z");
        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        LocalDateTime dateTime = LocalDateTime.of(2023, 10, 27, 15, 0);

        String result = schedulingService.priceDateLabel(dateTime);

        assertThat(result).isEqualTo("Today, ");
    }

    @Test
    void priceDateLabel_givenTomorrow_shouldReturnTomorrowWithComma() {
        Instant now = Instant.parse("2023-10-27T10:00:00.00Z");
        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        LocalDateTime dateTime = LocalDateTime.of(2023, 10, 28, 15, 0);

        String result = schedulingService.priceDateLabel(dateTime);

        assertThat(result).isEqualTo("Tomorrow, ");
    }

    @Test
    void priceDateLabel_givenNeitherTodayNorTomorrow_shouldReturnEmptyString() {
        Instant now = Instant.parse("2023-10-27T10:00:00.00Z");
        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        LocalDateTime dateTime = LocalDateTime.of(2023, 10, 29, 15, 0);

        String result = schedulingService.priceDateLabel(dateTime);

        assertThat(result).isEqualTo("");
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

    private void maxOutDailyMessageLimit() {
        for (int i = 0; i < SchedulingService.DAILY_MESSAGE_LIMIT; i++) {
            schedulingService.incrementMessageCountForToday();
        }
    }

}
