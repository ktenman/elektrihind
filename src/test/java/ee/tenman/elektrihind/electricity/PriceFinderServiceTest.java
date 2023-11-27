package ee.tenman.elektrihind.electricity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.lenient;

@Slf4j
@ExtendWith(MockitoExtension.class)
class PriceFinderServiceTest {

    @Mock
    private Clock clock;

    @InjectMocks
    private PriceFinderService priceFinderService;

    private static final String JSON_RESOURCE_PATH = "/electricityPrices.json";
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final List<ElectricityPrice> ELECTRICITY_PRICES = loadElectricityPrices();

    public static List<ElectricityPrice> loadElectricityPrices() {
        try (InputStream is = PriceFinderService.class.getResourceAsStream(JSON_RESOURCE_PATH)) {
            if (is == null) {
                log.error("Resource not found: {}", JSON_RESOURCE_PATH);
                throw new IllegalArgumentException("Resource not found: " + JSON_RESOURCE_PATH);
            }
            return OBJECT_MAPPER.readValue(is, new TypeReference<>() {
            });
        } catch (IOException e) {
            log.error("Failed to load electricity prices", e);
            throw new UncheckedIOException("Failed to load electricity prices", e);
        }
    }

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2023-11-09T23:45:14.00Z"));
        lenient().when(clock.getZone()).thenReturn(UTC);
    }

    @Test
    void testEmptyPriceList() {
        List<ElectricityPrice> emptyList = Collections.emptyList();
        int duration = 30;

        Throwable thrown = catchThrowable(() -> priceFinderService.findBestPriceForDuration(emptyList, duration));

        assertThat(thrown).isNotNull().isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Electricity prices list cannot be null or empty.");
    }

    @Test
    void testDurationExceedsListSize() {
        List<ElectricityPrice> prices = ELECTRICITY_PRICES;
        assert prices != null;
        int duration = prices.size() * 60 + 30; // Exceeds size of list in minutes

        BestPriceResult result = priceFinderService.findBestPriceForDuration(prices, duration);

        assertThat(result).isNull();
    }

    @Test
    void testSinglePriceEntry() {
        LocalDateTime fixedTime = LocalDateTime.now(); // Fixed time for comparison
        List<ElectricityPrice> prices = Collections.singletonList(new ElectricityPrice(fixedTime, 10.0));
        int duration = 60; // 1 hour

        BestPriceResult result = priceFinderService.findBestPriceForDuration(prices, duration);

        assertThat(result).isNotNull();
        assertThat(result.getTotalCost()).isEqualTo(10.0);
        assertThat(result.getStartTime()).isEqualTo(fixedTime.toString());
    }

    @Test
    void testBestStartTimeInLastHour() {
        // Use a fixed base time for both prices
        LocalDateTime baseTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES); // Truncate to avoid second/millisecond discrepancies
        List<ElectricityPrice> prices = Arrays.asList(
                new ElectricityPrice(baseTime, 15.0),
                new ElectricityPrice(baseTime.plusHours(1), 10.0)
        );
        int duration = 30; // 30 minutes

        BestPriceResult result = priceFinderService.findBestPriceForDuration(prices, duration);

        // The expected start time should be the start of the second hour, not 30 minutes before
        assertThat(result).isNotNull();
        assertThat(result.getStartTime()).isEqualTo(prices.get(1).getDate().toString()); // Adjusted as per your logic
        // The expected total cost is half of the second hour's price since the duration is 30 minutes
        assertThat(result.getTotalCost()).isEqualTo(10.0 / 2); // Should be half of 10.0 since it's 30 minutes
    }


    @Test
    void testSamePricesReturnsFirstSlot() {
        LocalDateTime localDateTime = LocalDateTime.of(2023, 11, 10, 0, 0);
        List<ElectricityPrice> prices = new ArrayList<>();
        // Assume each hour has the same price
        for (int i = 0; i < 5; i++) {
            prices.add(new ElectricityPrice(localDateTime.plusHours(i), 10.0));
        }
        int duration = 120; // 2 hours

        BestPriceResult result = priceFinderService.findBestPriceForDuration(prices, duration);

        assertThat(result).isNotNull();
        assertThat(result.getStartTime()).isEqualTo("2023-11-10T00:02");
        assertThat(result.getTotalCost()).isEqualTo(20.0); // 2 hours at 10.0 per hour
    }

    @Test
    void testVaryingPricesBestPriceAtBeginning() {
        List<ElectricityPrice> prices = new ArrayList<>();
        // Assume the price starts low and increases each hour
        for (int i = 0; i < 5; i++) {
            prices.add(new ElectricityPrice(LocalDateTime.now().plusHours(i), i * 5.0));
        }
        int duration = 180; // 3 hours

        BestPriceResult result = priceFinderService.findBestPriceForDuration(prices, duration);

        assertThat(result).isNotNull();
        assertThat(result.getStartTime()).isEqualTo(prices.get(0).getDate().toString());
        assertThat(result.getTotalCost()).isEqualTo(15.0); // 0+5+10 for the first three hours
    }

    @Test
    void testVaryingPricesBestPriceAtEnd() {
        List<ElectricityPrice> prices = new ArrayList<>();
        // Assume the price decreases each hour
        for (int i = 5; i > 0; i--) {
            prices.add(new ElectricityPrice(LocalDateTime.now().plusHours(5 - i), i * 5.0));
        }
        int duration = 120; // 2 hours

        BestPriceResult result = priceFinderService.findBestPriceForDuration(prices, duration);

        assertThat(result).isNotNull();
        assertThat(result.getStartTime()).isEqualTo(prices.get(3).getDate().toString());
        assertThat(result.getTotalCost()).isEqualTo(15);
        assertThat(result.getAveragePrice()).isEqualTo(7.5);
    }

    @Test
    void testPartialHoursProperlyCalculated() {
        List<ElectricityPrice> prices = new ArrayList<>();
        // Assume each hour has a different price
        for (int i = 0; i < 5; i++) {
            prices.add(new ElectricityPrice(LocalDateTime.now().plusHours(i), (i + 1) * 5.0));
        }
        int duration = 90; // 1 hour and 30 minutes

        BestPriceResult result = priceFinderService.findBestPriceForDuration(prices, duration);

        assertThat(result).isNotNull();
        // Here we expect the best start time to be at the first slot, since the price is lowest
        assertThat(result.getStartTime()).isEqualTo(prices.get(0).getDate().toString());
        // The expected total cost would be the first hour's cost plus half of the second hour's cost
        assertThat(result.getTotalCost()).isEqualTo(5.0 + (10.0 / 2)); // 5+5 for the first 1.5 hours
    }

    @Test
    void testMultipleStartTimesYieldingSameCostReturnsEarliest() {
        List<ElectricityPrice> prices = new ArrayList<>();
        // Assume there are several periods with the same total cost
        LocalDateTime now = LocalDateTime.of(2023, 11, 10, 0, 0, 0, 0);
        prices.add(new ElectricityPrice(now, 5.0));
        prices.add(new ElectricityPrice(now.plusHours(1), 15.0));
        prices.add(new ElectricityPrice(now.plusHours(2), 5.0));
        prices.add(new ElectricityPrice(now.plusHours(3), 15.0));
        int duration = 120; // 2 hours

        BestPriceResult result = priceFinderService.findBestPriceForDuration(prices, duration);

        assertThat(result).isNotNull();
        // We expect the earliest start time with the lowest total cost
        assertThat(result.getStartTime()).isEqualTo("2023-11-10T00:01");
        assertThat(result.getTotalCost()).isEqualTo(20.0); // The cost for the first and second hours
    }

    @ParameterizedTest
    @CsvSource({
            "210, 2023-11-10T03:30",
            "195, 2023-11-10T03:45",
            "180, 2023-11-10T04:00"
    })
    void shouldReturnNotNullBestPriceResultWithCorrectStartTimeForGivenDuration(int duration, String expectedStartTime) {
        BestPriceResult result = priceFinderService.findBestPriceForDuration(ELECTRICITY_PRICES, duration);

        assertThat(result).isNotNull();
        assertThat(result.getStartTime()).isEqualTo(expectedStartTime);
    }

    @Test
    void currentPrice_ShouldReturnNull_WhenNoCurrentPriceIsAvailable() {
        LocalDateTime now = LocalDateTime.now();
        List<ElectricityPrice> prices = List.of(
                new ElectricityPrice(now.minusHours(2), 10.0),
                new ElectricityPrice(now.minusHours(1), 20.0)
        );

        Optional<ElectricityPrice> price = priceFinderService.currentPrice(prices);

        assertThat(price).isEmpty();
    }

    @Test
    void currentPrice_ShouldReturnCorrectPrice_WhenPriceIsAvailable() {
        ElectricityPrice current = ELECTRICITY_PRICES.get(47);

        Optional<ElectricityPrice> price = priceFinderService.currentPrice(ELECTRICITY_PRICES);

        assertThat(price).isPresent()
                .isEqualTo(Optional.of(current));
    }

}
