package ee.tenman.elektrihind.car.auto24;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import ee.tenman.elektrihind.car.predict.PredictRequest;
import ee.tenman.elektrihind.car.predict.PredictService;
import ee.tenman.elektrihind.twocaptcha.TwoCaptchaSolverService;
import ee.tenman.elektrihind.utility.CaptchaSolver;
import ee.tenman.elektrihind.utility.FileToBase64;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static ee.tenman.elektrihind.config.RedisConfig.ONE_MONTH_CACHE_4;

@Service
@Slf4j
public class Auto24Service implements CaptchaSolver {

    private static final String SITE_KEY = "6Lf3qrkZAAAAAJLmqi1osY8lac0rLbAJItqEvZ0K";
    private static final String PAGE_URL = "https://www.auto24.ee/ostuabi/?t=soiduki-andmete-paring";

    private static final List<String> ACCEPTED_KEYS = List.of(
            "Kütusekulu keskmine (l/ 100 km)",
            "Kütusekulu linnas (l/100 km)",
            "Kütusekulu maanteel (l/100 km)"
    );

    @Resource(name = "fourThreadExecutor")
    private ExecutorService fourThreadExecutor;

    @Resource
    private TwoCaptchaSolverService twoCaptchaSolverService;

    @Resource
    private PredictService predictService;

    @SneakyThrows({IOException.class, InterruptedException.class})
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1500))
    @Cacheable(value = ONE_MONTH_CACHE_4, key = "#regNr")
    public LinkedHashMap<String, String> carPrice(String regNr) {
        log.info("Searching car price for regNr: {}", regNr);
        Selenide.open("https://www.auto24.ee/ostuabi/?t=soiduki-turuhinna-paring");
        TimeUnit.SECONDS.sleep(3);
        SelenideElement acceptCookies = $$(By.tagName("button")).findBy(Condition.text("Nõustun"));
        if (acceptCookies.exists()) {
            acceptCookies.click();
        }
        $(By.name("vpc_reg_nr")).setValue(regNr);
        $("button[type='submit']").click();
        File screenshot = $("#vpc_captcha").screenshot();
        assert screenshot != null;
        log.info("Solving price captcha for regNr: {}", regNr);
        byte[] screenshotBytes = Files.readAllBytes(screenshot.toPath());
        String encodedScreenshot = FileToBase64.encodeToBase64(screenshotBytes);
        String solveCaptcha = predictService.predict(new PredictRequest(encodedScreenshot))
                .orElseThrow(() -> new RuntimeException("Captcha not solved"));
        $(By.name("checksec1")).setValue(solveCaptcha);
        int count = 0;
        while ($(".errorMessage").exists() &&
                "Vale kontrollkood.".equalsIgnoreCase(Selenide.$(".errorMessage").text()) && count++ < 3) {
            log.warn("Invalid captcha for regNr: {}", regNr);
            screenshot = $("#vpc_captcha").screenshot();
            assert screenshot != null;
            log.info("Trying to solve price captcha for regNr: {}. Tries: {}", regNr, count);
            screenshotBytes = Files.readAllBytes(screenshot.toPath());
            encodedScreenshot = FileToBase64.encodeToBase64(screenshotBytes);
            solveCaptcha = predictService.predict(new PredictRequest(encodedScreenshot))
                    .orElseThrow(() -> new RuntimeException("Captcha not solved"));

            $(By.name("checksec1")).setValue(solveCaptcha);
            $("button[type='submit']").click();
        }
        SelenideElement errorMessage = $(".errorMessage");
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (errorMessage.exists() && !"Vale kontrollkood.".equalsIgnoreCase(errorMessage.text())) {
            log.error("Error while solving price captcha for regNr: {}. Error: {}", regNr, errorMessage.text());
            result.put("Reg nr", regNr);
            return result;
        }
        log.info("Price captcha solved for regNr: {}", regNr);
        String response = $$(By.tagName("div")).filter(Condition.text("Sõiduki keskmine hind"))
                .last()
                .parent()
                .text()
                .replace("\n", " ");
        Selenide.closeWindow();
        String[] split = response.split(": ");
        if (split.length <= 1) {
            result.put("Reg nr", regNr);
            return result;
        }
        result.put("Turuhind", split[1] + "\n");
        result.put("Reg nr", regNr);
        log.info("Price for regNr: {} is {}", regNr, response);
        fourThreadExecutor.submit(Selenide::closeWindow);
        return result;
    }

    @SneakyThrows({IOException.class, InterruptedException.class})
    public void solve(String regNr) {
        log.info("Searching car price for regNr: {}", regNr);
        Selenide.open("https://www.auto24.ee/ostuabi/?t=soiduki-turuhinna-paring");
        TimeUnit.SECONDS.sleep(3);
        SelenideElement acceptCookies = $$(By.tagName("button")).findBy(Condition.text("Nõustun"));
        if (acceptCookies.exists()) {
            acceptCookies.click();
        }
        $(By.name("vpc_reg_nr")).setValue(regNr);

        File screenshot = $("#vpc_captcha").screenshot();
        assert screenshot != null;
        log.info("Solving price captcha for regNr: {}", regNr);
        String encodedScreenshot = FileToBase64.encodeToBase64(Files.readAllBytes(screenshot.toPath()));

        String solveCaptcha = predictService.predict(new PredictRequest(encodedScreenshot))
                .orElseThrow(() -> new RuntimeException("Captcha not solved"));

        $(By.name("checksec1")).setValue(solveCaptcha);
        $("button[type='submit']").click();
        int count = 0;
        while ($(".errorMessage").exists() &&
                "Vale kontrollkood.".equalsIgnoreCase(Selenide.$(".errorMessage").text()) && count++ < 10) {
            log.warn("Invalid captcha for regNr: {}", regNr);
            screenshot = $("#vpc_captcha").screenshot();
            assert screenshot != null;
            log.info("Trying to solve price captcha for regNr: {}. Tries: {}", regNr, count);
            encodedScreenshot = FileToBase64.encodeToBase64(Files.readAllBytes(screenshot.toPath()));

            solveCaptcha = predictService.predict(new PredictRequest(encodedScreenshot))
                    .orElseThrow(() -> new RuntimeException("Captcha not solved"));

            $(By.name("checksec1")).setValue(solveCaptcha);
            $("button[type='submit']").click();
        }
        SelenideElement errorMessage = $(".errorMessage");
        if (errorMessage.exists() && !"Vale kontrollkood.".equalsIgnoreCase(errorMessage.text())) {
            log.error("Error while solving price captcha for regNr: {}. Error: {}", regNr, errorMessage.text());
            return;
        }
        log.info("Price captcha solved for regNr: {}", regNr);
        boolean success = $$(By.tagName("div")).filter(Condition.text("Sõiduki keskmine hind"))
                .last().exists();
        if (success) {
            FileUtils.copyFile(screenshot, new File("imagesYYY/" + solveCaptcha.toUpperCase() + ".png"));
        }
        fourThreadExecutor.submit(Selenide::closeWindow);
    }

    @SneakyThrows({InterruptedException.class})
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1500))
    public Map<String, String> carDetails(Map<String, String> carDetails, String captchaToken) {
        log.info("Searching car details for regNr: {}", carDetails.get("Reg nr"));
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
        executeJavaScript("document.getElementById('g-recaptcha-response').innerHTML = arguments[0];", captchaToken);
        $("button[type='submit']").click();
        ElementsCollection rows = Selenide.$(By.className("result")).findAll(By.tagName("tr"));
        for (int i = 0; i < rows.size(); i++) {
            ElementsCollection selenideElements = rows.get(i).$$("td");
            String key = selenideElements.get(0).getText();
            String value = selenideElements.get(1).getText();
            if (isAcceptedKey(key)) {
                key = key.replace("(l/ 100 km)", "")
                        .replace("(l/100 km)", "")
                        .trim();
                carDetails.put(key, value);
            }
        }

        List<String> texts = $$(By.tagName("h4"))
                .find(Condition.text("Ülevaatuste ajalugu"))
                .sibling(0)
                .findAll(By.tagName("tr")).get(1)
                .$$(By.tagName("td"))
                .texts();
        if (texts.size() > 1) {
            String labisoit = texts.get(4) + " (" + texts.get(0) + ")";
            carDetails.put("Läbisõit", labisoit);
        }

        log.info("Found car details for regNr: {}", regNr);
        fourThreadExecutor.submit(Selenide::closeWindow);
        return carDetails;
    }

    private boolean isAcceptedKey(String key) {
        return ACCEPTED_KEYS.stream().anyMatch(key::contains);
    }

    @Override
    public String getCaptchaToken() {
        log.info("Solving auto24 captcha");
        String token = twoCaptchaSolverService.solveCaptcha(SITE_KEY, PAGE_URL);
        log.info("Auto24 captcha solved");
        return token;
    }
}
