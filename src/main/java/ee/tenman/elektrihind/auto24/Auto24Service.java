package ee.tenman.elektrihind.auto24;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import ee.tenman.elektrihind.ark.ArkService;
import ee.tenman.elektrihind.lkf.LKFService;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static ee.tenman.elektrihind.config.RedisConfig.ONE_DAY_CACHE_2;

@Service
@Slf4j
public class Auto24Service {

    private static final String SITE_KEY = "6Lf3qrkZAAAAAJLmqi1osY8lac0rLbAJItqEvZ0K";
    private static final String PAGE_URL = "https://www.auto24.ee/ostuabi/?t=soiduki-andmete-paring";

    private static final List<String> ACCEPTED_KEYS = List.of(
            "Mark",
            "Kütusekulu keskmine (l/ 100 km)",
            "Kütusekulu linnas (l/100 km)",
            "Kütusekulu maanteel (l/100 km)",
            "Kaubanduslik nimetus",
            "Esmaregistreerimise kuupäev (B)",
            "Värvus",
            "Mootori töömaht (cm3)",
            "Mootori võimsus (kW)",
            "Mootori tüüp",
            "Käigukasti tüüp",
            "Veotelgi"
    );

    @Resource
    private RecaptchaSolverService recaptchaSolverService;

    @Resource
    private ArkService arkService;

    @Resource
    private LKFService lkfService;

    @Resource(name = "twoThreadExecutor")
    private ExecutorService twoThreadExecutor;

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
    public Map<String, String> carDetails(Map<String, String> carDetails) {
        String regNr = carDetails.get("Reg nr");
        String vin = carDetails.get("Vin");
        Selenide.open(PAGE_URL);
        TimeUnit.SECONDS.sleep(2);
        SelenideElement acceptCookies = $$(By.tagName("button")).findBy(Condition.text("Nõustun"));
        if (acceptCookies.exists()) {
            acceptCookies.click();
        }
        $(By.id("vin-inp")).setValue(vin);
        $(By.id("reg_nr-inp")).setValue(regNr);
        log.info("Solving car captcha for regNr: {}", regNr);
        String captchaToken = recaptchaSolverService.solveCaptcha(SITE_KEY, PAGE_URL);
        log.info("Car captcha solved for regNr: {}", regNr);
        executeJavaScript("document.getElementById('g-recaptcha-response').innerHTML = arguments[0];", captchaToken);
        $("button[type='submit']").click();
        ElementsCollection rows = Selenide.$(By.className("result")).findAll(By.tagName("tr"));
        for (int i = 0; i < rows.size(); i++) {
            ElementsCollection selenideElements = rows.get(i).$$("td");
            String key = selenideElements.get(0).getText();
            String value = selenideElements.get(1).getText();
            if (isAcceptedKey2(key)) {
                carDetails.put(key, value);
            }
        }

        ElementsCollection selenideElements = $$(By.tagName("h4"))
                .find(Condition.text("Ülevaatuste ajalugu"))
                .sibling(0)
                .findAll(By.tagName("tr"))
                .get(1).$$("td");
        String labisoit = selenideElements.get(4).text() + " (" + selenideElements.get(0).text() + ")";

        carDetails.put("Läbisõit", labisoit);

        Selenide.closeWindow();
        log.info("Found car details for regNr: {}", regNr);
        return carDetails;
    }

    @SneakyThrows({InterruptedException.class})
    public Map<String, String> carDetails(String regNr) {
        Selenide.open(PAGE_URL);
        TimeUnit.SECONDS.sleep(2);
        SelenideElement acceptCookies = $$(By.tagName("button")).findBy(Condition.text("Nõustun"));
        if (acceptCookies.exists()) {
            acceptCookies.click();
        }
        $(By.id("reg_nr-inp")).setValue(regNr);
        log.info("Solving car captcha for regNr: {}", regNr);
        String captchaToken = recaptchaSolverService.solveCaptcha(SITE_KEY, PAGE_URL);
        log.info("Car captcha solved for regNr: {}", regNr);
        executeJavaScript("document.getElementById('g-recaptcha-response').innerHTML = arguments[0];", captchaToken);
        $("button[type='submit']").click();
        ElementsCollection rows = $$("table.result tr");
        TreeMap<String, String> carDetails = new TreeMap<>();
        for (int i = 0; i < rows.size(); i++) {
            String key = rows.get(i).$$("td").get(0).getText();
            String value = rows.get(i).$$("td").get(1).getText();
            if (isAcceptedKey(key)) {
                carDetails.put(key, value);
            }
        }
        Selenide.closeWindow();
        log.info("Found car details for regNr: {}", regNr);
        return carDetails;
    }

    private boolean isAcceptedKey(String key) {
        return ACCEPTED_KEYS.stream().anyMatch(key::contains);
    }

    private boolean isAcceptedKey2(String key) {
        List<String> acceptableKeys = List.of(
                "Kütusekulu keskmine (l/ 100 km)",
                "Kütusekulu linnas (l/100 km)",
                "Kütusekulu maanteel (l/100 km)"
        );
        return acceptableKeys.stream().anyMatch(key::contains);
    }

    @SneakyThrows
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    @Cacheable(value = ONE_DAY_CACHE_2, key = "#regNr")
    public String search(String regNr) {

        Map<String, String> details = arkService.carDetails(regNr);
        details = carDetails(details);
        Map<String, String> crashes = lkfService.carDetails(regNr);
        details.putAll(crashes);
        String price = carPrice(regNr);

        return price + "\n\n" + details.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

//    @SneakyThrows
//    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
//    @Cacheable(value = ONE_DAY_CACHE, key = "#regNr")
//    public String search(String regNr) {
//        CompletableFuture<Map<String, String>> carDetailsFuture = CompletableFuture.supplyAsync(() -> carDetails(arkService.carDetails(regNr)), twoThreadExecutor);
//        CompletableFuture<String> carPriceFuture = CompletableFuture.supplyAsync(() -> carPrice(regNr), twoThreadExecutor);
//
//        carDetailsFuture.join();
//        carPriceFuture.join();
//
//        Map<String, String> details = carDetailsFuture.get();
//        String price = carPriceFuture.get();
//
//        return price + "\n\n" + details.entrySet().stream()
//                .map(entry -> entry.getKey() + ": " + entry.getValue())
//                .collect(Collectors.joining("\n"));
//    }

}
