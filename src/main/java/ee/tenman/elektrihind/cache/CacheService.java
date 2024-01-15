package ee.tenman.elektrihind.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import ee.tenman.elektrihind.apollo.ApolloKinoSession;
import ee.tenman.elektrihind.apollo.Option;
import ee.tenman.elektrihind.electricity.ElectricityPrice;
import ee.tenman.elektrihind.electricity.ElectricityPricesService;
import ee.tenman.elektrihind.movies.MovieDetails;
import ee.tenman.elektrihind.utility.JsonUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static ee.tenman.elektrihind.config.RedisConfiguration.APOLLO_KINO_CACHE;
import static ee.tenman.elektrihind.config.RedisConfiguration.ELECTRICITY_PRICES_CACHE;
import static ee.tenman.elektrihind.config.RedisConfiguration.MESSAGE_COUNTS_CACHE;
import static ee.tenman.elektrihind.config.RedisConfiguration.MOVIE_DETAILS_CACHE;
import static ee.tenman.elektrihind.config.RedisConfiguration.ONE_DAY_CACHE_1;
import static ee.tenman.elektrihind.config.RedisConfiguration.ONE_MONTH_CACHE_1;
import static ee.tenman.elektrihind.config.RedisConfiguration.ONE_MONTH_CACHE_2;
import static ee.tenman.elektrihind.config.RedisConfiguration.ONE_MONTH_CACHE_3;
import static ee.tenman.elektrihind.config.RedisConfiguration.ONE_MONTH_CACHE_4;
import static ee.tenman.elektrihind.config.RedisConfiguration.ONE_MONTH_CACHE_5;
import static ee.tenman.elektrihind.config.RedisConfiguration.ONE_YEAR_CACHE_1;
import static ee.tenman.elektrihind.config.RedisConfiguration.SESSIONS_CACHE;
import static ee.tenman.elektrihind.utility.GlobalConstants.TEST_PROFILE;

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

    public static final String AUTOMATIC_FETCHING_KEY = "automaticFetching";
    private static final String DURATIONS_KEY = "durations";
    private static final String LAST_EURIBOR_MESSAGE_SENT_KEY = "lastEuriborMessageSentDate";
    private static final String LAST_EURIBOR_RATE_KEY = "lastEuriborRate";
    private static final String SESSIONS_KEY = "sessions";

    @Resource
    private CacheManager cacheManager;

    @PostConstruct
    public void init() {
        log.info("Initializing CacheService...");
        if (List.of(environment.getActiveProfiles()).contains(TEST_PROFILE)) {
            log.info("Skipping initialization in test profile");
            return;
        }

        try {
            latestPrices = getElectricityPrices();
            if (latestPrices.isEmpty()) {
                log.info("No electricity prices found in cache. Fetching...");
                latestPrices = electricityPricesService.fetchDailyPrices();
                log.info("Fetched {} prices.", latestPrices.size());
                setElectricityPrices(latestPrices);
            }
            log.info("Latest prices initialized with {} entries", latestPrices.size());
        } catch (Exception e) {
            log.error("Error during initialization: {}", e.getMessage(), e);
        }
        log.info("CacheService initialization completed");
    }

    private List<ElectricityPrice> getElectricityPrices() {
        log.info("Getting electricity prices from cache");
        Optional<String> dataJson = Optional.ofNullable(cacheManager.getCache(ELECTRICITY_PRICES_CACHE))
                .map(c -> c.get(ELECTRICITY_PRICES_CACHE, String.class));
        if (dataJson.isEmpty()) {
            log.info("Electricity prices not found in cache");
            return new ArrayList<>();
        }

        TypeReference<List<ElectricityPrice>> typeReference = new TypeReference<>() {
        };
        List<ElectricityPrice> result = JsonUtil.deserializeList(dataJson.get(), typeReference);
        log.info("Electricity prices retrieved from cache");
        return result;
    }

    public void setElectricityPrices(List<ElectricityPrice> electricityPrices) {
        log.info("Setting electricity prices in cache");
        String serializedData = JsonUtil.serializeList(electricityPrices);
        Optional.ofNullable(cacheManager.getCache(ELECTRICITY_PRICES_CACHE))
                .ifPresent(c -> c.put(ELECTRICITY_PRICES_CACHE, serializedData));
        log.info("Electricity prices set in cache");
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
        return !lastMessageSentDate.equals(LocalDate.now(clock));
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

    public void updateSessionsJson(String sessionsJson) {
        Optional.ofNullable(cacheManager.getCache(SESSIONS_CACHE))
                .ifPresent(c -> c.put(SESSIONS_KEY, sessionsJson));
        log.info("Updated sessions cache");
    }

    public Map<UUID, ApolloKinoSession> getRebookingSessions() {
        log.info("Getting rebooking sessions from cache");
        Optional<String> sessionsJson = Optional.ofNullable(cacheManager.getCache(SESSIONS_CACHE))
                .map(c -> c.get(SESSIONS_KEY, String.class));
        if (sessionsJson.isEmpty()) {
            log.info("Rebooking sessions not found in cache");
            return new HashMap<>();
        }

        TypeReference<Map<UUID, ApolloKinoSession>> typeReference = new TypeReference<>() {
        };
        Map<UUID, ApolloKinoSession> result = JsonUtil.deserializeMap(sessionsJson.get(), typeReference);
        log.info("Rebooking sessions retrieved from cache");
        return result;
    }

    public void updateRebookingSessions(Map<UUID, ApolloKinoSession> sessions) {
        log.info("Updating rebooking sessions in cache");
        String serializedSessions = JsonUtil.serializeMap(sessions);
        Optional.ofNullable(cacheManager.getCache(SESSIONS_CACHE))
                .ifPresent(c -> c.put(SESSIONS_KEY, serializedSessions));
        log.info("Rebooking sessions updated in cache");
    }

    public void updateApolloKinoData(Map<LocalDate, List<Option>> data) {
        log.info("Updating Apollo Kino data in cache");
        String serializedData = JsonUtil.serializeMap(data);
        Optional.ofNullable(cacheManager.getCache(APOLLO_KINO_CACHE))
                .ifPresent(c -> c.put(APOLLO_KINO_CACHE, serializedData));
        log.info("Apollo Kino data updated in cache");
    }

    public Map<LocalDate, List<Option>> getApolloKinoData() {
        log.info("Getting Apollo Kino data from cache");
        Optional<String> dataJson = Optional.ofNullable(cacheManager.getCache(APOLLO_KINO_CACHE))
                .map(c -> c.get(APOLLO_KINO_CACHE, String.class));
        if (dataJson.isEmpty()) {
            log.info("Apollo Kino data not found in cache");
            return new HashMap<>();
        }

        TypeReference<Map<LocalDate, List<Option>>> typeReference = new TypeReference<>() {
        };
        Map<LocalDate, List<Option>> result = JsonUtil.deserializeMap(dataJson.get(), typeReference);
        log.info("Apollo Kino data retrieved from cache");
        return result;
    }

    public void removeRebookingSession(UUID uuid) {
        log.info("Removing rebooking session {} from cache", uuid);
        Map<UUID, ApolloKinoSession> rebookingSessions = getRebookingSessions();
        rebookingSessions.remove(uuid);
        updateRebookingSessions(rebookingSessions);
        log.info("Rebooking session {} removed from cache", uuid);
    }

    public void addRebookingSession(UUID sessionId, ApolloKinoSession session) {
        log.info("Adding rebooking session {} to cache", sessionId);
        Map<UUID, ApolloKinoSession> rebookingSessions = getRebookingSessions();
        rebookingSessions.put(sessionId, session);
        updateRebookingSessions(rebookingSessions);
        log.info("Rebooking session {} added to cache", sessionId);
    }

    public Optional<MovieDetails> fetchMovieDetails(String title) {
        return Optional.ofNullable(cacheManager.getCache(MOVIE_DETAILS_CACHE))
                .map(c -> c.get(title, MovieDetails.class));
    }

    public void saveMovieDetails(String title, MovieDetails movieDetails) {
        log.info("Saving movie details for title: {}", title);
        Optional.ofNullable(cacheManager.getCache(MOVIE_DETAILS_CACHE))
                .ifPresent(c -> c.put(title, movieDetails));
        log.info("Movie details saved for title: {}", title);
    }

}
