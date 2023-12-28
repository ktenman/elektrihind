package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.car.ark.ArkService;
import ee.tenman.elektrihind.car.auto24.Auto24Service;
import ee.tenman.elektrihind.car.lkf.LKFService;
import ee.tenman.elektrihind.car.scrapeninja.ScrapeninjaService;
import ee.tenman.elektrihind.electricity.CarSearchUpdateListener;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static ee.tenman.elektrihind.config.RedisConfig.ONE_MONTH_CACHE_2;
import static ee.tenman.elektrihind.config.RedisConfig.ONE_MONTH_CACHE_3;

@Service
@Slf4j
public class CarSearchService {

    private static final String REGISTRATION_DOCUMENT = "Registreerimistunnistus";
    private static final String AUTO_MAKS_URL = "https://www.err.ee/1609128527/uuendatud-kalkulaator-vaata-kui-suur-tuleb-sinu-automaks";

    @Resource
    private ArkService arkService;

    @Resource
    private LKFService lkfService;

    @Resource
    private Auto24Service auto24Service;

    @Resource
    private ScrapeninjaService scrapeninjaService;

    @Resource(name = "fourThreadExecutor")
    private ExecutorService fourThreadExecutor;

    private static void removeRedundantInformation(Map<String, String> response) {
        if (response.isEmpty()) {
            return;
        }
        response.remove(REGISTRATION_DOCUMENT);
        response.remove("Logo");
        response.remove("Täismass");
        response.remove("Tühimass");
        response.remove("CO2 (NEDC)");
        response.remove("CO2 (WLTP)");
    }

    @SneakyThrows
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1500))
    @Cacheable(value = ONE_MONTH_CACHE_2, key = "#regNr")
    public Map<String, String> search(String regNr) {
        CompletableFuture<LinkedHashMap<String, String>> carPriceFuture = CompletableFuture.supplyAsync(() -> auto24Service.carPrice(regNr), fourThreadExecutor);
        CompletableFuture<String> arkCaptchaTokenFuture = CompletableFuture.supplyAsync(() -> arkService.getCaptchaToken(), fourThreadExecutor);
        CompletableFuture<String> auto24CaptchaTokenFuture = CompletableFuture.supplyAsync(() -> auto24Service.getCaptchaToken(), fourThreadExecutor);
        CompletableFuture<String> lkfCaptchaTokenFuture = CompletableFuture.supplyAsync(() -> lkfService.getCaptchaToken(), fourThreadExecutor);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(carPriceFuture, arkCaptchaTokenFuture, auto24CaptchaTokenFuture, lkfCaptchaTokenFuture);
        allFutures.join();

        LinkedHashMap<String, String> response = carPriceFuture.get();

        String arkCaptchaToken = arkCaptchaTokenFuture.get();
        Map<String, String> arkDetails = arkService.carDetails(regNr, arkCaptchaToken);
        response.putAll(arkDetails);

        String auto24CaptchaToken = auto24CaptchaTokenFuture.get();
        Map<String, String> auto24Details = auto24Service.carDetails(response, auto24CaptchaToken);
        response.putAll(auto24Details);

        String lkfCaptchaToken = lkfCaptchaTokenFuture.get();
        Map<String, String> crashes = lkfService.carDetails(regNr, lkfCaptchaToken);
        response.putAll(crashes);

        removeRedundantInformation(response);

        return response;
    }

    @SneakyThrows
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    @Cacheable(value = ONE_MONTH_CACHE_3, key = "#regNr")
    public Map<String, String> searchV2(String regNr, CarSearchUpdateListener updateListener) {
        int timeout = 7;
        TimeUnit timeUnit = TimeUnit.MINUTES;
        CompletableFuture<LinkedHashMap<String, String>> carPriceFuture = CompletableFuture.supplyAsync(() -> auto24Service.carPrice(regNr), fourThreadExecutor)
                .orTimeout(timeout, timeUnit);

        CompletableFuture<String> arkCaptchaTokenFuture = CompletableFuture.supplyAsync(() -> arkService.getCaptchaToken(), fourThreadExecutor)
                .orTimeout(timeout, timeUnit);
        CompletableFuture<String> auto24CaptchaTokenFuture = CompletableFuture.supplyAsync(() -> auto24Service.getCaptchaToken(), fourThreadExecutor)
                .orTimeout(timeout, timeUnit);
        CompletableFuture<String> lkfCaptchaTokenFuture = CompletableFuture.supplyAsync(() -> lkfService.getCaptchaToken(), fourThreadExecutor)
                .orTimeout(timeout, timeUnit);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(carPriceFuture, arkCaptchaTokenFuture, auto24CaptchaTokenFuture, lkfCaptchaTokenFuture)
                .orTimeout(timeout, timeUnit);
        allFutures.join();

        Map<String, String> response = carPriceFuture.get();
        updateListener.onUpdate(response, false);

        String arkCaptchaToken = arkCaptchaTokenFuture.get();
        Map<String, String> arkDetails = CompletableFuture.supplyAsync(() -> arkService.carDetails(regNr, arkCaptchaToken, response, updateListener), fourThreadExecutor)
                .orTimeout(timeout, timeUnit)
                .get();
        response.putAll(arkDetails);
        if (arkDetails.size() <= 1 && response.size() == 1) {
            response.put("Viga", "Andmeid ei leitud '" + regNr + "' kohta");
            return response;
        } else if (arkDetails.size() <= 1) {
            return response;
        }

        String auto24CaptchaToken = auto24CaptchaTokenFuture.get();
        Map<String, String> scrapeNinjaDetails = CompletableFuture.supplyAsync(() -> scrapeninjaService.scrape(arkDetails.get("Vin"), regNr, auto24CaptchaToken), fourThreadExecutor)
                .orTimeout(timeout, timeUnit)
                .get();
        response.putAll(scrapeNinjaDetails);
        updateListener.onUpdate(response, false);

        String lkfCaptchaToken = lkfCaptchaTokenFuture.get();
        Map<String, String> crashes = CompletableFuture.supplyAsync(() -> lkfService.carDetails(regNr, lkfCaptchaToken), fourThreadExecutor)
                .orTimeout(timeout, timeUnit)
                .get();
        response.putAll(crashes);
        if (!crashes.isEmpty()) {
            updateListener.onUpdate(response, false);
        }

        if (!response.containsKey("Läbisõit") && response.containsKey("Vin")) {
            String captchaToken = auto24Service.getCaptchaToken();
            Map<String, String> auto24details = CompletableFuture.supplyAsync(() -> auto24Service.carDetails(response, captchaToken), fourThreadExecutor)
                    .orTimeout(timeout, timeUnit)
                    .get();
            response.putAll(auto24details);
            updateListener.onUpdate(response, false);
        }

        if (response.containsKey("Läbisõit")) {
            String string = response.get("Läbisõit");
            if (string.toLowerCase().contains("maanteeamet")) {
                response.remove("Läbisõit");
            }
        }

        removeRedundantInformation(response);

        return response;
    }

    @SneakyThrows
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    @Cacheable(value = ONE_MONTH_CACHE_3, key = "#regNr")
    public Map<String, String> searchV3(String regNr, CarSearchUpdateListener updateListener) {
        int timeout = 5;
        TimeUnit timeUnit = TimeUnit.MINUTES;

        // Parallel fetching of car price and captcha tokens
        CompletableFuture<LinkedHashMap<String, String>> carPriceFuture = CompletableFuture.supplyAsync(() -> auto24Service.carPrice(regNr), fourThreadExecutor)
                .orTimeout(timeout, timeUnit);

        CompletableFuture<String> arkCaptchaTokenFuture = CompletableFuture.supplyAsync(() -> arkService.getCaptchaToken(), fourThreadExecutor)
                .orTimeout(timeout, timeUnit);
        CompletableFuture<String> auto24CaptchaTokenFuture = CompletableFuture.supplyAsync(() -> auto24Service.getCaptchaToken(), fourThreadExecutor)
                .orTimeout(timeout, timeUnit);
        CompletableFuture<String> lkfCaptchaTokenFuture = CompletableFuture.supplyAsync(() -> lkfService.getCaptchaToken(), fourThreadExecutor)
                .orTimeout(timeout, timeUnit);

        // Fetch arkDetails and then start scrapeninjaService.scrape
        CompletableFuture<Map<String, String>> arkDetailsFuture = arkCaptchaTokenFuture.thenApplyAsync(token ->
                arkService.carDetails(regNr, token, carPriceFuture.join(), updateListener), fourThreadExecutor
        ).orTimeout(timeout, timeUnit);

        CompletableFuture<Map<String, String>> scrapeninjaDetailsFuture = arkDetailsFuture.thenApplyAsync(arkDetails ->
                scrapeninjaService.scrape(arkDetails.get("Vin"), regNr, auto24CaptchaTokenFuture.join()), fourThreadExecutor
        ).orTimeout(timeout, timeUnit);

        // Fetch lkfDetails in parallel
        CompletableFuture<Map<String, String>> lkfDetailsFuture = lkfCaptchaTokenFuture.thenApplyAsync(token ->
                lkfService.carDetails(regNr, token), fourThreadExecutor
        ).orTimeout(timeout, timeUnit);

        // Combine all futures
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(carPriceFuture, arkDetailsFuture, scrapeninjaDetailsFuture, lkfDetailsFuture)
                .orTimeout(timeout, timeUnit);

        allFutures.join();

        Map<String, String> response = new LinkedHashMap<>();
        response.putAll(carPriceFuture.get());
        response.putAll(arkDetailsFuture.get());
        response.putAll(scrapeninjaDetailsFuture.get());
        response.putAll(lkfDetailsFuture.get());

        updateListener.onUpdate(response, false);

        if (!response.containsKey("Läbisõit") && response.containsKey("Vin")) {
            String captchaToken = auto24Service.getCaptchaToken();
            Map<String, String> auto24details = CompletableFuture.supplyAsync(() -> auto24Service.carDetails(response, captchaToken), fourThreadExecutor)
                    .orTimeout(timeout, timeUnit)
                    .get();
            response.putAll(auto24details);
            updateListener.onUpdate(response, false);
        }

        if (response.containsKey("Läbisõit")) {
            String string = response.get("Läbisõit");
            if (string.toLowerCase().contains("maanteeamet")) {
                response.remove("Läbisõit");
            }
        }

        removeRedundantInformation(response);

        return response;
    }


}
