package ee.tenman.elektrihind;

import ee.tenman.elektrihind.electricity.ElectricityPrice;
import ee.tenman.elektrihind.electricity.ElectricityPricesService;
import ee.tenman.elektrihind.utility.GlobalConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    Clock clock;

    @Mock
    Environment environment;

    @Mock
    ElectricityPricesService electricityPricesService;

    @InjectMocks
    CacheService cacheService;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2023-10-27T10:00:00.00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        ReflectionTestUtils.setField(cacheService, "cacheFilePath", "/app/cache/cache_file.dat");
    }

    @Test
    void canSendMessageToday_withNoMessagesSent_shouldReturnTrue() {
        assertThat(cacheService.canSendMessageToday()).isTrue();
    }

    @Test
    void canSendMessageToday_withDailyLimitReached_shouldReturnFalse() {
        maxOutDailyMessageLimit();

        assertThat(cacheService.canSendMessageToday()).isFalse();
    }

    @Test
    void getMessageCount_whenDateNotInCache_shouldReturnZero() {
        LocalDate someDate = LocalDate.of(2022, 1, 1);

        assertThat(cacheService.getMessageCount(someDate)).isZero();
    }

    @Test
    void getMessageCount_whenDateInCache_shouldReturnCount() {
        LocalDate someDate = LocalDate.of(2022, 1, 1);
        cacheService.getMessageCountPerDay().put(someDate, 5);

        assertThat(cacheService.getMessageCount(someDate)).isEqualTo(5);
    }


    @Test
    void init_whenInTestProfile_shouldSkipInitialization() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test", GlobalConstants.TEST_PROFILE});

        cacheService.init();

        verify(electricityPricesService, never()).fetchDailyPrices();
    }

    @Test
    void init_whenLatestPricesIsEmpty_shouldFetchDailyPrices() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(electricityPricesService.fetchDailyPrices()).thenReturn(List.of(new ElectricityPrice()));

        cacheService.init();

        verify(electricityPricesService).fetchDailyPrices();
        assertThat(cacheService.getLatestPrices()).isNotEmpty();
    }

    @Test
    void init_whenLatestPricesIsNotEmpty_shouldNotFetchDailyPrices() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        cacheService.setLatestPrices(List.of(new ElectricityPrice()));

        cacheService.init();

        verify(electricityPricesService, never()).fetchDailyPrices();
    }

    private void maxOutDailyMessageLimit() {
        for (int i = 0; i < CacheService.DAILY_MESSAGE_LIMIT; i++) {
            cacheService.incrementMessageCountForToday();
        }
    }

}
