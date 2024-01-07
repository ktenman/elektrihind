package ee.tenman.elektrihind.telegram;

import ee.tenman.elektrihind.electricity.ElectricityPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JavaElekterTelegramServiceTest {

    @Mock
    Clock clock;

    @InjectMocks
    JavaElekterTelegramService javaElekterTelegramService;


    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2023-10-27T10:00:00.00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }

    @Test
    void formatPricesForTelegram_givenPrices_shouldFormatCorrectly() {
        String result = javaElekterTelegramService.formatPricesForTelegram(createSamplePrices());

        assertThat(result).contains("Tomorrow, the most expensive:", "Today, the cheapest:");
    }


    @Test
    void priceDateLabel_givenToday_shouldReturnTodayWithComma() {
        Instant now = Instant.parse("2023-10-27T10:00:00.00Z");
        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        LocalDateTime dateTime = LocalDateTime.of(2023, 10, 27, 15, 0);

        String result = javaElekterTelegramService.priceDateLabel(dateTime);

        assertThat(result).isEqualTo("Today, ");
    }

    @Test
    void priceDateLabel_givenTomorrow_shouldReturnTomorrowWithComma() {
        Instant now = Instant.parse("2023-10-27T10:00:00.00Z");
        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        LocalDateTime dateTime = LocalDateTime.of(2023, 10, 28, 15, 0);

        String result = javaElekterTelegramService.priceDateLabel(dateTime);

        assertThat(result).isEqualTo("Tomorrow, ");
    }

    @Test
    void priceDateLabel_givenNeitherTodayNorTomorrow_shouldReturnEmptyString() {
        Instant now = Instant.parse("2023-10-27T10:00:00.00Z");
        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        LocalDateTime dateTime = LocalDateTime.of(2023, 10, 29, 15, 0);

        String result = javaElekterTelegramService.priceDateLabel(dateTime);

        assertThat(result).isEmpty();
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

}
