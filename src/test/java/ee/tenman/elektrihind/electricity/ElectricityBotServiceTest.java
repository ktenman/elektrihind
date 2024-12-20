package ee.tenman.elektrihind.electricity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.tenman.elektrihind.FileUtils;
import ee.tenman.elektrihind.cache.CacheService;
import ee.tenman.elektrihind.config.FeesConfiguration;
import ee.tenman.elektrihind.telegram.JavaElekterTelegramService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;

import static ee.tenman.elektrihind.electricity.ElectricityBotService.DURATION_PATTERN;
import static ee.tenman.elektrihind.electricity.ElectricityBotService.UNKNOWN_USERNAME;
import static ee.tenman.elektrihind.electricity.PriceFinderServiceTest.ELECTRICITY_PRICES;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElectricityBotServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private Update update;

    @Mock
    private FeesConfiguration feesConfiguration;

    @Mock
    private PriceFinderService priceFinderService;

    @Mock
    private Message message;

    @Mock
    private CacheService cacheService;

    @Mock
    private ExecutorService singleThreadExecutor;

    @Mock
    private JavaElekterTelegramService javaElekterTelegramService;

    @Captor
    private ArgumentCaptor<SendMessage> sendMessageCaptor;

    @Mock
    private Clock clock;

    @InjectMocks
    private ElectricityBotService botService;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2023-11-09T23:45:14.00Z"));
        lenient().when(clock.getZone()).thenReturn(UTC);
        lenient().when(update.getMessage()).thenReturn(message);
        lenient().when(message.getChatId()).thenReturn(12345L);

        lenient().when(feesConfiguration.getFixedSurcharge()).thenReturn(BigDecimal.valueOf(8.56));
        lenient().when(feesConfiguration.getMonthlyFee()).thenReturn(BigDecimal.valueOf(1.23));
        lenient().when(feesConfiguration.getDayDistributionFee()).thenReturn(BigDecimal.valueOf(0.52));
        lenient().when(feesConfiguration.getNightDistributionFee()).thenReturn(BigDecimal.valueOf(0.11));
        lenient().when(feesConfiguration.getApartmentMonthlyFee()).thenReturn(BigDecimal.valueOf(6.39));
        lenient().when(feesConfiguration.getRenewableEnergyFee()).thenReturn(BigDecimal.valueOf(5.32));
        lenient().when(feesConfiguration.getElectricityExciseTax()).thenReturn(BigDecimal.valueOf(0.021));
        lenient().when(feesConfiguration.getSalesTax()).thenReturn(BigDecimal.valueOf(1.22));
        botService.getValidUsernames().add(UNKNOWN_USERNAME);
    }

    @Test
    void calculateTotalCost_ShouldCalculateCostsCorrectly() {
        List<String[]> data = List.of(
                new String[]{"01.01.2023 00:00", "A", "100", "150.00"},
                new String[]{"02.01.2023 00:00", "B", "200", "200.00"}
        );

        CostCalculationResult result = botService.calculateTotalCost(data);

        assertNotNull(result);
        assertThat(result.totalKwh()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(result.totalCost()).isGreaterThan(new BigDecimal("0.00"));
    }

    @Test
    void calculateTotalCost_ShouldIgnoreInvalidRows() {
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"invalid_date", "A", "not_a_number", "not_a_price"});
        data.add(new String[]{"01.01.2023 00:00", "B", "200", "200.00"}); // Add a valid row to check if the method processes valid rows correctly

        CostCalculationResult result = botService.calculateTotalCost(data);

        assertNotNull(result);
        assertThat(result.totalKwh()).isEqualByComparingTo(new BigDecimal("200.00")); // This should only include the valid row
        assertThat(result.totalCost()).isGreaterThan(new BigDecimal("0.00")); // This should reflect the cost from the valid row
    }

    @Test
    void whenUpdateHasNoMessage_thenNothingHappens() {
        when(update.hasMessage()).thenReturn(false);

        botService.onUpdateReceived(update);

        verify(message, never()).hasText();
    }

    @Test
    @Disabled
    @SneakyThrows(TelegramApiException.class)
    void whenMessageHasStartCommand_thenReplyWithWelcome() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start");
        String welcomeMessage = "Hello! I am an electricity bill calculator bot. Please send me a CSV file.";

        ElectricityBotService spyBotService = spy(botService);
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
        when(cacheService.getLatestPrices()).thenReturn(ELECTRICITY_PRICES);
        when(javaElekterTelegramService.formatPricesForTelegram(any())).thenReturn("Sample Formatted Prices");
        when(priceFinderService.currentPrice(any())).thenReturn(Optional.of(ELECTRICITY_PRICES.get(47)));

        ElectricityBotService spyBotService = spy(botService);
        spyBotService.onUpdateReceived(update);

        verify(spyBotService, times(1)).execute(sendMessageCaptor.capture());
        SendMessage sentMessage = sendMessageCaptor.getAllValues().getFirst();
        assertThat(sentMessage.getChatId()).isEqualTo(String.valueOf(message.getChatId()));
//        assertThat(sentMessage.getText()).contains("Current electricity price is 10.08 cents/kWh.");
//        assertThat(sentMessage.getText()).contains("Upcoming prices:");
//        assertThat(sentMessage.getText()).contains("2023-11-10 00:00 - 4.8");
//        assertThat(sentMessage.getText()).contains("2023-11-11 00:00 - 4.81");
//        assertThat(sentMessage.getText()).contains("Sample Formatted Prices");
    }

    @Test
    @SneakyThrows(TelegramApiException.class)
    void whenMessageHasDurationPattern_thenReplyWithBestPrice() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("parim hind 30 min");

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime firstHour = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), now.getHour(), 0);
        List<ElectricityPrice> mockPrices = List.of(
                ElectricityPrice.builder()
                        .date(firstHour)
                        .price(56.0)
                        .build(),
                ElectricityPrice.builder()
                        .date(firstHour.plusHours(1))
                        .price(5.0)
                        .build(),
                ElectricityPrice.builder()
                        .date(firstHour.plusHours(2))
                        .price(4.0)
                        .build()
        );
        when(cacheService.getLatestPrices()).thenReturn(mockPrices);
        when(priceFinderService.findBestPriceForDuration(any(), anyInt())).thenReturn(BestPriceResult.builder()
                .startTime(firstHour.plusHours(2))
                .totalCost(2.0)
                .averagePrice(4.0)
                .build());
        when(priceFinderService.calculateImmediateCost(any(), anyInt())).thenReturn(BigDecimal.valueOf(15.05));


        ElectricityBotService spyBotService = Mockito.spy(botService);

        spyBotService.onUpdateReceived(update);

        String expectedMessage = ("`Best time to start is 2023-11-10 01:00 with average price of 4.0 cents/kWh. Total " +
                "cost is 2.0 cents. In 1 hours! Start consuming immediately at 2023-11-09 23:45. Total cost is 15.05 " +
                "cents with average price of 30.1 cents/kWh. 7.53x more expensive to start immediately.`");
        verify(spyBotService, times(1)).execute(sendMessageCaptor.capture()); // This should probably be realBotService
        String actualMessage = sendMessageCaptor.getValue().getText().replaceAll("\\s+", " ").trim();
//        assertThat(actualMessage).isEqualTo(expectedMessage);
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
        ElectricityBotService spyBotService = Mockito.spy(botService);

        spyBotService.onUpdateReceived(update);

        verify(spyBotService).execute(sendMessageCaptor.capture());
        SendMessage sentMessage = sendMessageCaptor.getValue();
        assertThat(sentMessage.getChatId()).isEqualTo(String.valueOf(message.getChatId()));
        assertThat(sentMessage.getText()).contains("Please send a CSV or image file.");
    }

    @Test
    void whenMessageHasCSVDocument_thenHandleCSVDocument() {
        when(update.hasMessage()).thenReturn(true);
        when(message.hasText()).thenReturn(false);
        when(message.hasDocument()).thenReturn(true);
        Document document = mock(Document.class);
        when(message.getDocument()).thenReturn(document);
        when(document.getFileName()).thenReturn("file.csv");
        ElectricityBotService spyBotService = spy(botService);
        doNothing().when(spyBotService).handleCsvDocument(any(Document.class), anyLong());

        spyBotService.onUpdateReceived(update);

        verify(spyBotService).handleCsvDocument(document, message.getChatId());
    }

    @Test
    void testFormatBestPriceResponse() {
        LocalDateTime startTime = LocalDateTime.of(2023, 11, 11, 12, 0);
        BestPriceResult bestPrice = new BestPriceResult(startTime, 20.5, 5.75);

        String response = botService.formatBestPriceResponse(bestPrice);

        String expected = "Best time to start is 2023-11-11 12:00 with average price of 5.75 cents/kWh. Total cost is 20.5 cents. In 36 hours!";
        assertThat(response).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "parim hind 200 min, 200",
            "parim hind 180min, 180",
            "parim hind 3 h 27 min, 207",
            "parim hind 3:27, 207",
    })
    void testDurationInMinutes(String input, int expectedDurationInMinutes) {
        ElectricityBotService service = new ElectricityBotService();
        Matcher matcher = DURATION_PATTERN.matcher(input);
        matcher.find();

        int result = service.durationInMinutes(matcher);

        assertThat(result).isEqualTo(expectedDurationInMinutes);
    }

    @Test
    @SneakyThrows
    void getElectricityPriceResponse() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2023-10-26T10:45:14.00Z"));
        List<ElectricityPrice> electricityPrices = OBJECT_MAPPER.readValue(FileUtils.readFileAsString("__files/daily_prices_response.json"), new TypeReference<>() {
        });

        when(cacheService.getLatestPrices()).thenReturn(electricityPrices);
        when(priceFinderService.currentPrice(any())).thenReturn(Optional.of(electricityPrices.get(10)));
        when(javaElekterTelegramService.formatPricesForTelegram(any())).thenReturn("Sample Formatted Prices");

        String response = botService.getElectricityPriceResponse();

        assertThat(response).contains("Current electricity price is 19.44 cents/kWh.")
                .contains("Upcoming prices:")
                .contains("Sample Formatted Prices");
    }

}
