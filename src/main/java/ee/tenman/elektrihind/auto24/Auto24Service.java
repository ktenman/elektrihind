package ee.tenman.elektrihind.auto24;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.executeJavaScript;

@Service
@Slf4j
public class Auto24Service {

    @Resource(name = "twoThreadExecutor")
    private ExecutorService twoThreadExecutor;

    @Resource
    private RecaptchaSolverService recaptchaSolverService;

    @SneakyThrows({IOException.class, InterruptedException.class})
    public String carPrice(String regNr) {
        Selenide.open("https://www.auto24.ee/ostuabi/?t=soiduki-turuhinna-paring");
        TimeUnit.SECONDS.sleep(2);
        SelenideElement acceptCookies = $$(By.tagName("button")).findBy(Condition.text("Nõustun"));
        if (acceptCookies.exists()) {
            acceptCookies.click();
        }
        $(By.name("vpc_reg_nr")).setValue(regNr);

        File screenshot = $("#vpc_captcha").screenshot();
        assert screenshot != null;
        log.info("Solving price captcha for regNr: {}", regNr);
        String solveCaptcha = recaptchaSolverService.solveCaptcha(Files.readAllBytes(screenshot.toPath()));
        $(By.name("checksec1")).setValue(solveCaptcha);
        $("button[type='submit']").click();
        int count = 0;
        while ($(".errorMessage").exists() && count++ < 7) {
            log.warn("Invalid captcha for regNr: {}", regNr);
            screenshot = $("#vpc_captcha").screenshot();
            assert screenshot != null;
            log.info("Solving captcha for regNr: {}", regNr);
            solveCaptcha = recaptchaSolverService.solveCaptcha(Files.readAllBytes(screenshot.toPath()));
            $(By.name("checksec1")).setValue(solveCaptcha);
            $("button[type='submit']").click();
        }
        log.info("Price captcha solved for regNr: {}", regNr);
        String response = $$(By.tagName("div")).filter(Condition.text("Sõiduki keskmine hind"))
                .last()
                .parent()
                .text()
                .replace("\n", " ");
        Selenide.closeWindow();
        log.info("Price for regNr: {} is {}", regNr, response);
        return response;
    }

    @SneakyThrows({InterruptedException.class})
    public Map<String, String> carDetails(String regNr) {
        Selenide.open("https://www.auto24.ee/ostuabi/?t=soiduki-andmete-paring");
        TimeUnit.SECONDS.sleep(2);
        SelenideElement acceptCookies = $$(By.tagName("button")).findBy(Condition.text("Nõustun"));
        if (acceptCookies.exists()) {
            acceptCookies.click();
        }
        $(By.id("reg_nr-inp")).setValue(regNr);
        log.info("Solving car captcha for regNr: {}", regNr);
        String captchaToken = recaptchaSolverService.solveCaptcha();
        log.info("Car captcha solved for regNr: {}", regNr);
        executeJavaScript("document.getElementById('g-recaptcha-response').innerHTML = arguments[0];", captchaToken);
        $("button[type='submit']").click();
        ElementsCollection rows = $$("table.result tr");
        Map<String, String> carDetails = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            String key = rows.get(i).$$("td").get(0).getText();
            String value = rows.get(i).$$("td").get(1).getText();
            carDetails.put(key, value);
        }
        Selenide.closeWindow();
        log.info("Found car details for regNr: {}", regNr);
        return carDetails;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    public String search(String regNr) {
        long startTime = System.nanoTime();

        CompletableFuture<String> carPriceFuture = CompletableFuture.supplyAsync(() -> carPrice(regNr), twoThreadExecutor);
        CompletableFuture<Map<String, String>> carDetailsFuture = CompletableFuture.supplyAsync(() -> carDetails(regNr), twoThreadExecutor);

        CompletableFuture<String> combinedFuture = carPriceFuture.thenCombine(carDetailsFuture, (price, details) -> {
            String detailsString = details.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + entry.getValue())
                    .collect(Collectors.joining("\n"));
            return price + "\n\n" + detailsString;
        });

        String result = combinedFuture.join();
        long endTime = System.nanoTime();
        double durationSeconds = (endTime - startTime) / 1_000_000_000.0;

        return result + "\n\nTask Duration: " + String.format("%.1f seconds", durationSeconds);
    }

}
