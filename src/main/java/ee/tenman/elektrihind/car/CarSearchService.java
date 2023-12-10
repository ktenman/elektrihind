package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.car.ark.ArkService;
import ee.tenman.elektrihind.car.auto24.Auto24Service;
import ee.tenman.elektrihind.car.lkf.LKFService;
import ee.tenman.elektrihind.car.scrapeninja.ScrapeninjaService;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ee.tenman.elektrihind.config.RedisConfig.ONE_DAY_CACHE_2;
import static ee.tenman.elektrihind.config.RedisConfig.ONE_DAY_CACHE_4;

@Service
public class CarSearchService {

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

    @SneakyThrows
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1500))
    @Cacheable(value = ONE_DAY_CACHE_2, key = "#regNr")
    public String search(String regNr) {
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

        return response.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    @SneakyThrows
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    @Cacheable(value = ONE_DAY_CACHE_4, key = "#regNr")
    public String search2(String regNr) {
        int timeout = 4;
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

        String arkCaptchaToken = arkCaptchaTokenFuture.get();
        Map<String, String> arkDetails = CompletableFuture.supplyAsync(() -> arkService.carDetails(regNr, arkCaptchaToken), fourThreadExecutor)
                .orTimeout(timeout, timeUnit)
                .get();
        response.putAll(arkDetails);

        String auto24CaptchaToken = auto24CaptchaTokenFuture.get();
        Map<String, String> scrapeNinjaDetails = CompletableFuture.supplyAsync(() -> scrapeninjaService.scrape(arkDetails.get("Vin"), regNr, auto24CaptchaToken), fourThreadExecutor)
                .orTimeout(timeout, timeUnit)
                .get();
        response.putAll(scrapeNinjaDetails);

        String lkfCaptchaToken = lkfCaptchaTokenFuture.get();
        Map<String, String> crashes = CompletableFuture.supplyAsync(() -> lkfService.carDetails(regNr, lkfCaptchaToken), fourThreadExecutor)
                .orTimeout(timeout, timeUnit)
                .get();
        response.putAll(crashes);

        if (!response.containsKey("Läbisõit") || !response.containsKey("Kütusekulu keskmine")) {
            String captchaToken = auto24Service.getCaptchaToken();
            Map<String, String> auto24details = CompletableFuture.supplyAsync(() -> auto24Service.carDetails(response, captchaToken), fourThreadExecutor)
                    .orTimeout(timeout, timeUnit)
                    .get();
            response.putAll(auto24details);
        }
        return response.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

}
