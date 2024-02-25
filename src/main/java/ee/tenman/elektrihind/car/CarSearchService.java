package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.cache.CacheService;
import ee.tenman.elektrihind.car.ark.ArkService;
import ee.tenman.elektrihind.car.auto24.Auto24Service;
import ee.tenman.elektrihind.car.lkf.LKFService;
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

import static ee.tenman.elektrihind.config.RedisConfiguration.ONE_MONTH_CACHE_3;

@Service
@Slf4j
public class CarSearchService {

    private static final String REGISTRATION_DOCUMENT = "Registreerimistunnistus";
    private static final String ODOMETER = "Läbisõit";

    @Resource
    private ArkService arkService;

    @Resource
    private LKFService lkfService;

    @Resource
    private Auto24Service auto24Service;

    @Resource
    private CacheService cacheService;

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
        response.remove("Kategooria tähis");
    }

    @SneakyThrows
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    @Cacheable(value = ONE_MONTH_CACHE_3, key = "#regNr")
    public Map<String, String> searchV2(String regNr, CarSearchUpdateListener updateListener) {
        int timeout = 7;
        TimeUnit timeUnit = TimeUnit.MINUTES;

//        CompletableFuture<LinkedHashMap<String, String>> carPriceFuture = CompletableFuture.supplyAsync(() -> auto24Service.carPrice(regNr), fourThreadExecutor)
//                .orTimeout(timeout, timeUnit);
        CompletableFuture<String> arkCaptchaTokenFuture = CompletableFuture.supplyAsync(() -> arkService.getCaptchaToken(), fourThreadExecutor)
                .orTimeout(timeout, timeUnit);
        CompletableFuture<String> lkfCaptchaTokenFuture = CompletableFuture.supplyAsync(() -> lkfService.getCaptchaToken(), fourThreadExecutor)
                .orTimeout(timeout, timeUnit);
        
        Map<String, String> response = new LinkedHashMap<>();
        response.put("Reg nr", regNr);

        updateListener.onUpdate(response, false);

        String arkCaptchaToken = arkCaptchaTokenFuture.join();
        Map<String, String> arkDetails = CompletableFuture.supplyAsync(() -> arkService.carDetails(regNr, arkCaptchaToken, response, updateListener), fourThreadExecutor)
                .orTimeout(timeout, timeUnit)
                .join();
        response.putAll(arkDetails);
        if (arkDetails.size() <= 1 && response.size() == 1) {
            response.put("Viga", "Andmeid ei leitud '" + regNr + "' kohta");
            return response;
        } else if (arkDetails.size() <= 1) {
            return response;
        }
        updateListener.onUpdate(response, false);

        String lkfCaptchaToken = lkfCaptchaTokenFuture.join();
        Map<String, String> crashes = CompletableFuture.supplyAsync(() -> lkfService.carDetails(regNr, lkfCaptchaToken), fourThreadExecutor)
                .orTimeout(timeout, timeUnit)
                .get();
        response.putAll(crashes);
        if (!crashes.isEmpty()) {
            updateListener.onUpdate(response, false);
        }

        if (response.containsKey(ODOMETER)) {
            String string = response.remove(ODOMETER);
            response.put(ODOMETER, string);
        }

        removeRedundantInformation(response);
        updateListener.onUpdate(response, false);

        return response;
    }


}
