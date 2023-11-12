package ee.tenman.elektrihind.electricity;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static ee.tenman.elektrihind.electricity.PriceFinderTest.ELECTRICITY_PRICES;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElekterBotServiceTest {

    @Mock
    private Update update;

    @Mock
    private Message message;

    @Captor
    private ArgumentCaptor<SendMessage> sendMessageCaptor;

    @Mock
    private ElectricityPricesService electricityPricesService;

    @Mock
    private Clock clock;

    @InjectMocks
    private ElekterBotService botService;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2023-11-09T23:45:14.00Z"));
        lenient().when(clock.getZone()).thenReturn(UTC);
        lenient().when(update.getMessage()).thenReturn(message);
        lenient().when(message.getChatId()).thenReturn(12345L);
    }

    @Test
    void currentPrice_ShouldReturnCorrectPrice_WhenPriceIsAvailable() {
        Double price = botService.currentPrice(ELECTRICITY_PRICES);

        assertThat(price).isEqualTo(10.08);
    }

    @Test
    void calculateTotalCost_ShouldCalculateCostsCorrectly() {
        List<String[]> data = List.of(
                new String[]{"01.01.2023 00:00", "A", "100", "150.00"},
                new String[]{"02.01.2023 00:00", "B", "200", "200.00"}
        );

        CostCalculationResult result = botService.calculateTotalCost(data);

        assertNotNull(result);
        assertThat(result.getTotalCost()).isPositive();
        assertThat(result.getTotalKwh()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(result.getTotalCost()).isGreaterThan(new BigDecimal("0.00"));
    }

    @Test
    void currentPrice_ShouldReturnNull_WhenNoCurrentPriceIsAvailable() {
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        List<ElectricityPrice> prices = List.of(
                new ElectricityPrice(LocalDateTime.now(clock).minusHours(2), 10.0),
                new ElectricityPrice(LocalDateTime.now(clock).minusHours(1), 20.0)
        );

        Double price = botService.currentPrice(prices);

        assertThat(price).isNull();
    }

    @Test
    void calculateTotalCost_ShouldIgnoreInvalidRows() {
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"invalid_date", "A", "not_a_number", "not_a_price"});
        data.add(new String[]{"01.01.2023 00:00", "B", "200", "200.00"}); // Add a valid row to check if the method processes valid rows correctly

        CostCalculationResult result = botService.calculateTotalCost(data);

        assertNotNull(result);
        assertThat(result.getTotalCost()).isPositive(); // Assuming that the method adds a fixed surcharge and this should be reflected in total cost
        assertThat(result.getTotalKwh()).isEqualByComparingTo(new BigDecimal("200.00")); // This should only include the valid row
        assertThat(result.getTotalCost()).isGreaterThan(new BigDecimal("0.00")); // This should reflect the cost from the valid row
    }

    @Test
    void whenUpdateHasNoMessage_thenNothingHappens() {
        when(update.hasMessage()).thenReturn(false);

        botService.onUpdateReceived(update);

        verify(message, never()).hasText();
    }

    @Test
    @SneakyThrows(TelegramApiException.class)
    void whenMessageHasStartCommand_thenReplyWithWelcome() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start");
        String welcomeMessage = "Hello! I am an electricity bill calculator bot. Please send me a CSV file.";

        ElekterBotService spyBotService = spy(botService);
        spyBotService.onUpdateReceived(update);

        verify(spyBotService).execute(sendMessageCaptor.capture());
        SendMessage sentMessage = sendMessageCaptor.getValue();
        assertThat(sentMessage.getChatId()).isEqualTo(String.valueOf(message.getChatId()));
        assertThat(sentMessage.getText()).isEqualTo(welcomeMessage);
    }

    @Test
    @SneakyThrows(TelegramApiException.class)
    void whenMessageHasElektrihind_thenReplyWithCurrentPrice() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("elektrihind");
        when(electricityPricesService.fetchDailyPrices()).thenReturn(ELECTRICITY_PRICES);

        ElekterBotService spyBotService = spy(botService);
        spyBotService.onUpdateReceived(update);

        verify(spyBotService).execute(sendMessageCaptor.capture());
        SendMessage sentMessage = sendMessageCaptor.getValue();
        assertThat(sentMessage.getChatId()).isEqualTo(String.valueOf(message.getChatId()));
        assertThat(sentMessage.getText()).contains("Current electricity price is 10.08 cents/kWh.");
    }

    @Test
    @SneakyThrows(TelegramApiException.class)
    void whenMessageHasDurationPattern_thenReplyWithBestPrice() {
        // Arrange
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("parim hind 30 min");
        long chatId = message.getChatId();

        LocalDateTime futureTime = LocalDateTime.now(clock).plusMinutes(30);
        List<ElectricityPrice> mockPrices = List.of(
                ElectricityPrice.builder()
                        .date(futureTime)
                        .price(5.0)
                        .build(),
                ElectricityPrice.builder()
                        .date(futureTime.plusMinutes(30))
                        .price(4.0)
                        .build()
        );
        when(electricityPricesService.fetchDailyPrices()).thenReturn(mockPrices);

        BestPriceResult mockBestPriceResult = BestPriceResult.builder()
                .startTime(futureTime)
                .totalCost(2.25)
                .averagePrice(4.5) // Normally, you would calculate this based on the duration and cost
                .build();

        ElekterBotService spyBotService = Mockito.spy(botService);

        // Act
        spyBotService.onUpdateReceived(update);

        // Assert
        String expectedMessage = "Best time to start is " + mockBestPriceResult.getStartTime() +
                " with average price of " + mockBestPriceResult.getAveragePrice() +
                " cents/kWh. Total cost is " + mockBestPriceResult.getTotalCost() + " EUR.";
        verify(spyBotService).execute(sendMessageCaptor.capture()); // This should probably be realBotService
    }

    @Test
    @SneakyThrows(TelegramApiException.class)
    void whenMessageHasDocumentButNotCSV_thenAskForCSV() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(false);
        when(message.hasDocument()).thenReturn(true);
        Document document = mock(Document.class);
        when(message.getDocument()).thenReturn(document);
        when(document.getFileName()).thenReturn("not_a_csv.txt");
        ElekterBotService spyBotService = Mockito.spy(botService);

        spyBotService.onUpdateReceived(update);

        verify(spyBotService).execute(sendMessageCaptor.capture());
        SendMessage sentMessage = sendMessageCaptor.getValue();
        assertThat(sentMessage.getChatId()).isEqualTo(String.valueOf(message.getChatId()));
        assertThat(sentMessage.getText()).contains("Please send a CSV file.");
    }

    @Test
    void whenMessageHasCSVDocument_thenHandleCSVDocument() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(false);
        when(message.hasDocument()).thenReturn(true);
        Document document = mock(Document.class);
        when(message.getDocument()).thenReturn(document);
        when(document.getFileName()).thenReturn("file.csv");
        ElekterBotService spyBotService = spy(botService);
        doNothing().when(spyBotService).handleCsvDocument(any(Document.class), anyLong());

        spyBotService.onUpdateReceived(update);

        verify(spyBotService).handleCsvDocument(document, message.getChatId());
    }

    @Test
    void testFormatBestPriceResponse() {
        LocalDateTime startTime = LocalDateTime.of(2023, 11, 11, 12, 0);
        BestPriceResult bestPrice = new BestPriceResult(startTime, 20.5, 5.75);

        String response = botService.formatBestPriceResponse(bestPrice);

        String expected = "Best time to start is " + startTime +
                " with average price of 5.75 cents/kWh. " +
                "Total cost is 20.5 EUR. In 36 hours!";
        assertThat(response).isEqualTo(expected);
    }

}
