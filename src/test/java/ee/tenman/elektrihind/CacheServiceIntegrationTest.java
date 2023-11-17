package ee.tenman.elektrihind;

import ee.tenman.elektrihind.electricity.ElekterBotService;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@IntegrationTest
class CacheServiceIntegrationTest {

    @MockBean
    ElekterBotService elekterBotService;
    @MockBean
    Clock clock;
    @Resource
    CacheService cacheService;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        cacheService.clearCache();
        String tempCacheFilePath = tempDir.resolve("cache_file.json").toString();
        ReflectionTestUtils.setField(cacheService, "cacheFilePath", tempCacheFilePath);
        lenient().when(clock.instant()).thenReturn(Instant.parse("2023-10-27T10:00:00.00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }

    @Test
    @SneakyThrows(IOException.class)
    void testCacheSerialization() {
        LocalDate testDate = LocalDate.now(clock);
        cacheService.incrementMessageCountForToday();

        assertThat(cacheService.getMessageCountPerDay().getIfPresent(testDate)).isEqualTo(1);

        cacheService.saveCacheToFile();

        Path filePath = Paths.get(cacheService.getCacheFilePath());
        assertThat(Files.exists(filePath)).isTrue();
        assertThat(Files.size(filePath)).isPositive();
    }

    @Test
    void testCacheDeserialization() {
        LocalDate testDate = LocalDate.now(clock);
        cacheService.incrementMessageCountForToday();
        cacheService.saveCacheToFile();

        cacheService.clearCache();
        assertThat(cacheService.getMessageCountPerDay().getIfPresent(testDate)).isNull();

        cacheService.loadCacheFromFile();

        assertThat(cacheService.getMessageCount(testDate)).isEqualTo(1);
    }

    @Test
    void testGetMessageCount() {
        LocalDate testDate = LocalDate.now(clock);
        cacheService.incrementMessageCountForToday();

        assertThat(cacheService.getMessageCount(testDate)).isEqualTo(1);
    }

    @Test
    void testCanSendMessageToday_whenFalse() {
        assertThat(cacheService.canSendMessageToday()).isTrue();

        for (int i = 0; i < CacheService.DAILY_MESSAGE_LIMIT; i++) {
            cacheService.incrementMessageCountForToday();
        }

        assertThat(cacheService.canSendMessageToday()).isFalse();
    }

}
