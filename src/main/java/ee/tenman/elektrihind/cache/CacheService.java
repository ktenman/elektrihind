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
import org.springframework.cache.annotation.Caching;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ee.tenman.elektrihind.config.RedisConfig.MESSAGE_COUNTS_CACHE;
import static ee.tenman.elektrihind.config.RedisConfig.ONE_DAY_CACHE_1;
import static ee.tenman.elektrihind.config.RedisConfig.ONE_MONTH_CACHE_1;
import static ee.tenman.elektrihind.config.RedisConfig.ONE_MONTH_CACHE_2;
import static ee.tenman.elektrihind.config.RedisConfig.ONE_MONTH_CACHE_3;
import static ee.tenman.elektrihind.config.RedisConfig.ONE_MONTH_CACHE_4;
import static ee.tenman.elektrihind.config.RedisConfig.ONE_MONTH_CACHE_5;
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

    private static final String AUTOMAKS_KEY = "automaks";
    public static final String AUTOMATIC_FETCHING_KEY = "automaticFetching";
    private static final String DURATIONS_KEY = "durations";
    private static final String LAST_EURIBOR_MESSAGE_SENT_KEY = "lastEuriborMessageSentDate";
    private static final String LAST_EURIBOR_RATE_KEY = "lastEuriborRate";


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
        Boolean result = Boolean.TRUE;
        if (cache != null && cache.get(key) != null) {
            result = Optional.ofNullable(cache.get(key))
                    .map(Cache.ValueWrapper::get)
                    .map(Boolean.class::cast)
                    .orElse(Boolean.TRUE);
        }
        setBooleanInCache(key, result);
        return result;
    }

    private void setBooleanInCache(String key, Boolean value) {
        Cache cache = cacheManager.getCache(ONE_YEAR_CACHE_1);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    public List<Double> getDurations() {
        Cache cache = cacheManager.getCache(ONE_YEAR_CACHE_1);
        if (cache != null) {
            return Optional.ofNullable(cache.get(DURATIONS_KEY, List.class))
                    .orElse(new ArrayList<Double>());
        }
        return new ArrayList<>();
    }

    public void addDuration(Double newDuration) {
        List<Double> durations = getDurations();
        durations.add(newDuration);
        Cache cache = cacheManager.getCache(ONE_YEAR_CACHE_1);
        if (cache != null) {
            cache.put(DURATIONS_KEY, durations);
        }
    }

    @Caching(evict = {
            @CacheEvict(value = ONE_MONTH_CACHE_1, key = "#regNr"),
            @CacheEvict(value = ONE_MONTH_CACHE_2, key = "#regNr"),
            @CacheEvict(value = ONE_MONTH_CACHE_3, key = "#regNr"),
            @CacheEvict(value = ONE_MONTH_CACHE_4, key = "#regNr"),
            @CacheEvict(value = ONE_MONTH_CACHE_5, key = "#regNr")
    })
    public void evictCacheEntry(String regNr) {
        log.info("Evicting cache entries for regNr: {}", regNr);
    }

    public boolean canSendEuriborMessageToday() {
        LocalDate lastMessageSentDate = getLastMessageSentDate();
        if (lastMessageSentDate == null) {
            log.info("No last Euribor message sent date found in cache. Sending message...");
            return true;
        }
        if (lastMessageSentDate.equals(LocalDate.now(clock))) {
            log.info("Euribor message sending limit reached for today.");
            return false;
        } else {
            log.info("Last Euribor message sent date is not today. Sending message...");
            return true;
        }
    }

    public void updateLastMessageSentDate() {
        log.info("Updating last Euribor message sent date in cache to today");
        setLastMessageSentDate(LocalDate.now(clock));
    }

    private LocalDate getLastMessageSentDate() {
        Cache cache = cacheManager.getCache(ONE_DAY_CACHE_1);
        if (cache != null && cache.get(LAST_EURIBOR_MESSAGE_SENT_KEY) != null) {
            return Optional.ofNullable(cache.get(LAST_EURIBOR_MESSAGE_SENT_KEY))
                    .map(Cache.ValueWrapper::get)
                    .map(String.class::cast)
                    .map(LocalDate::parse)
                    .orElse(null);
        }
        return null;
    }

    private void setLastMessageSentDate(LocalDate date) {
        Cache cache = cacheManager.getCache(ONE_DAY_CACHE_1);
        if (cache != null) {
            cache.put(LAST_EURIBOR_MESSAGE_SENT_KEY, date.toString());
        }
    }

    public BigDecimal getLastEuriborRate() {
        Cache cache = cacheManager.getCache(ONE_YEAR_CACHE_1);
        if (cache != null && cache.get(LAST_EURIBOR_RATE_KEY) != null) {
            return Optional.ofNullable(cache.get(LAST_EURIBOR_RATE_KEY))
                    .map(Cache.ValueWrapper::get)
                    .map(BigDecimal.class::cast)
                    .orElse(null);
        }
        return null;
    }

    public void setLastEuriborRate(BigDecimal rate) {
        Cache cache = cacheManager.getCache(ONE_YEAR_CACHE_1);
        if (cache != null) {
            cache.put(LAST_EURIBOR_RATE_KEY, rate);
            log.info("Updated last Euribor rate in cache to {}", rate);
        } else {
            log.error("Cache is null, unable to update last Euribor rate");
        }
    }

}
