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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
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
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    @Cacheable(value = ONE_DAY_CACHE_2, key = "#regNr")
    public String search(String regNr) {

        CompletableFuture<Map<String, String>> carPriceFuture = CompletableFuture.supplyAsync(() -> auto24Service.carPrice(regNr), fourThreadExecutor);
        CompletableFuture<String> arkCaptchaTokenFuture = CompletableFuture.supplyAsync(() -> arkService.getCaptchaToken(), fourThreadExecutor);
        CompletableFuture<String> auto24CaptchaTokenFuture = CompletableFuture.supplyAsync(() -> auto24Service.getCaptchaToken(), fourThreadExecutor);
        CompletableFuture<String> lkfCaptchaTokenFuture = CompletableFuture.supplyAsync(() -> lkfService.getCaptchaToken(), fourThreadExecutor);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(carPriceFuture, arkCaptchaTokenFuture, auto24CaptchaTokenFuture, lkfCaptchaTokenFuture);
        allFutures.join();

        Map<String, String> response = carPriceFuture.get();

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

        CompletableFuture<Map<String, String>> carPriceFuture = CompletableFuture.supplyAsync(() -> auto24Service.carPrice(regNr), fourThreadExecutor);
        CompletableFuture<String> arkCaptchaTokenFuture = CompletableFuture.supplyAsync(() -> arkService.getCaptchaToken(), fourThreadExecutor);
        CompletableFuture<String> auto24CaptchaTokenFuture = CompletableFuture.supplyAsync(() -> auto24Service.getCaptchaToken(), fourThreadExecutor);
        CompletableFuture<String> lkfCaptchaTokenFuture = CompletableFuture.supplyAsync(() -> lkfService.getCaptchaToken(), fourThreadExecutor);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(carPriceFuture, arkCaptchaTokenFuture, auto24CaptchaTokenFuture, lkfCaptchaTokenFuture);
        allFutures.join();

        Map<String, String> response = carPriceFuture.get();

        String arkCaptchaToken = arkCaptchaTokenFuture.get();
        Map<String, String> arkDetails = arkService.carDetails(regNr, arkCaptchaToken);
        response.putAll(arkDetails);

        String auto24CaptchaToken = auto24CaptchaTokenFuture.get();
        Map<String, String> auto24Details = scrapeninjaService.scrape(arkDetails.get("Vin"), regNr, auto24CaptchaToken);
        response.putAll(auto24Details);

        String lkfCaptchaToken = lkfCaptchaTokenFuture.get();
        Map<String, String> crashes = lkfService.carDetails(regNr, lkfCaptchaToken);
        response.putAll(crashes);

        return response.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }
}
