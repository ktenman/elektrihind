package ee.tenman.elektrihind.cache;

import ee.tenman.elektrihind.electricity.ElectricityPrice;
import ee.tenman.elektrihind.electricity.ElectricityPricesService;
import ee.tenman.elektrihind.utility.GlobalConstants;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ee.tenman.elektrihind.config.RedisConfig.MESSAGE_COUNTS_CACHE;
import static ee.tenman.elektrihind.config.RedisConfig.ONE_YEAR_CACHE_1;

@Service
@Slf4j
public class CacheService {

    static final int DAILY_MESSAGE_LIMIT = 1;

    @Resource
    private Clock clock;
    @Resource
    private ElectricityPricesService electricityPricesService;
    @Resource
    private Environment environment;
    @Resource
    private CacheHelperService cacheHelperService;
    @Setter
    @Getter
    private List<ElectricityPrice> latestPrices = new ArrayList<>();

    private Boolean automaticFetchingEnabled = Boolean.FALSE;
    public static final String AUTOMAKS_KEY = "automaks";
    public static final String AUTOMATIC_FETCHING_KEY = "automaticFetching";
    private Boolean automaksEnabled = Boolean.FALSE;

    @Resource
    private CacheManager cacheManager;

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
        log.info("CacheService initialization completed");
    }

    public boolean canSendMessageToday() {
        LocalDate today = LocalDate.now(clock);
        return cacheHelperService.getMessageCount(today) < DAILY_MESSAGE_LIMIT;
    }

    public void incrementMessageCountForToday() {
        LocalDate today = LocalDate.now(clock);
        log.info("Incrementing message count for date: {}", today);
        cacheHelperService.incrementMessageCount(today, cacheHelperService.getMessageCount(today));
        log.info("Message count for today incremented.");
    }

    @CacheEvict(value = MESSAGE_COUNTS_CACHE, allEntries = true)
    public void clearCache() {
        log.info("Message counts cache cleared successfully.");
    }

    public boolean isAutomaticFetchingEnabled() {
        return getBooleanFromCache(AUTOMATIC_FETCHING_KEY);
    }

    public void setAutomaticFetchingEnabled(Boolean automaticFetchingEnabled) {
        setBooleanInCache(AUTOMATIC_FETCHING_KEY, automaticFetchingEnabled);
    }

    public boolean isAutomaksEnabled() {
        return getBooleanFromCache(AUTOMAKS_KEY);
    }

    public void setAutomaksEnabled(Boolean automaticFetchingEnabled) {
        setBooleanInCache(AUTOMAKS_KEY, automaticFetchingEnabled);
    }

    private boolean getBooleanFromCache(String key) {
        Cache cache = cacheManager.getCache(ONE_YEAR_CACHE_1);
        if (cache != null && cache.get(key) != null) {
            return Optional.ofNullable(cache.get(key))
                    .map(Cache.ValueWrapper::get)
                    .map(Boolean.class::cast)
                    .orElse(Boolean.FALSE);
        }
        return false;
    }

    private void setBooleanInCache(String key, Boolean value) {
        Cache cache = cacheManager.getCache(ONE_YEAR_CACHE_1);
        if (cache != null) {
            cache.put(key, value);
        }
    }

}
