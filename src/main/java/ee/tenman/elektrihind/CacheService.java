package ee.tenman.elektrihind;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import ee.tenman.elektrihind.electricity.ElectricityPrice;
import ee.tenman.elektrihind.electricity.ElectricityPricesService;
import ee.tenman.elektrihind.util.GlobalConstants;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CacheService {

    static final int DAILY_MESSAGE_LIMIT = 2;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Getter
    private final Cache<LocalDate, Integer> messageCountPerDay = CacheBuilder.newBuilder()
            .expireAfterWrite(7, TimeUnit.DAYS)
            .build();

    @Resource
    private Clock clock;
    @Resource
    private ElectricityPricesService electricityPricesService;
    @Resource
    private Environment environment;
    @Setter
    @Getter
    private List<ElectricityPrice> latestPrices = new ArrayList<>();

    @Value("${cache.file.path:/app/cache/cache_file.json}")
    @Getter
    private String cacheFilePath;

    @PostConstruct
    public void init() {
        log.info("Initializing CacheService...");
        if (List.of(environment.getActiveProfiles()).contains(GlobalConstants.TEST_PROFILE)) {
            log.info("Skipping initialization in test profile");
            return;
        }

        try {
            if (latestPrices.isEmpty()) {
                latestPrices = electricityPricesService.fetchDailyPrices();
                log.info("Latest prices initialized with {} entries", latestPrices.size());
            }
        } catch (Exception e) {
            log.error("Error during initialization: {}", e.getMessage(), e);
        }

        loadCacheFromFile();
        log.info("CacheService initialization completed");
    }

    public int getMessageCount(LocalDate date) {
        Integer count = messageCountPerDay.getIfPresent(date);
        if (count == null) {
            log.debug("No message count found for date: {}", date);
            return 0;
        }
        log.debug("Retrieved message count for date {}: {}", date, count);
        return count;
    }

    public boolean canSendMessageToday() {
        LocalDate today = LocalDate.now(clock);
        return getMessageCount(today) < DAILY_MESSAGE_LIMIT;
    }

    public void incrementMessageCountForToday() {
        log.info("Incrementing message count for today...");
        LocalDate today = LocalDate.now(clock);
        int currentCount = getMessageCount(today);
        messageCountPerDay.put(today, currentCount + 1);
        log.info("Message count for today incremented. Current count: {}", currentCount + 1);
        saveCacheToFile();
    }

    void saveCacheToFile() {
        try (FileWriter writer = new FileWriter(cacheFilePath)) {
            Map<LocalDate, Integer> cacheMap = new HashMap<>(messageCountPerDay.asMap());
            String json = OBJECT_MAPPER.writeValueAsString(cacheMap);
            writer.write(json);
            log.debug("Cache saved to file in JSON format");
        } catch (IOException e) {
            log.error("Error saving cache to file", e);
        }
    }


    void loadCacheFromFile() {
        try (FileReader reader = new FileReader(cacheFilePath)) {
            Map<LocalDate, Integer> loadedMap = OBJECT_MAPPER.readValue(reader, new TypeReference<>() {
            });
            messageCountPerDay.putAll(loadedMap);
            log.debug("Cache loaded from file");
        } catch (FileNotFoundException e) {
            log.warn("Cache file not found, starting with empty cache");
        } catch (IOException e) {
            log.error("Error loading cache from file", e);
        }
    }


    public void clearCache() {
        messageCountPerDay.invalidateAll();
        log.info("Cache cleared successfully.");
    }

}
