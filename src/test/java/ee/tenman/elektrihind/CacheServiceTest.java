package ee.tenman.elektrihind;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    Clock clock;

    @InjectMocks
    CacheService cacheService;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2023-10-27T10:00:00.00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());
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

    private void maxOutDailyMessageLimit() {
        for (int i = 0; i < CacheService.DAILY_MESSAGE_LIMIT; i++) {
            cacheService.incrementMessageCountForToday();
        }
    }

}
