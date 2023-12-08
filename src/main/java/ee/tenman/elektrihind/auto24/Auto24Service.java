package ee.tenman.elektrihind.auto24;

import jakarta.annotation.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
public class Auto24Service {

    @Resource
    private Auto24PriceService auto24PriceService;

    @Resource
    private Auto24DetailsService auto24DetailsService;

    @Resource
    private ExecutorService executor;

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    public String search(String regNr) {
        CompletableFuture<String> priceFuture = CompletableFuture.supplyAsync(
                () -> auto24PriceService.carPrice(regNr), executor);
        CompletableFuture<Map<String, String>> detailsFuture = CompletableFuture.supplyAsync(
                () -> auto24DetailsService.carDetails(regNr), executor);

        return priceFuture.thenCombine(detailsFuture, (price, details) -> price + "\n\n" + details.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"))).join(); // Block and get the result
    }

}
