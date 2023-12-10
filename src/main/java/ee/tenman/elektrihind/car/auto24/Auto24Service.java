package ee.tenman.elektrihind.car.auto24;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import ee.tenman.elektrihind.recaptcha.RecaptchaSolverService;
import ee.tenman.elektrihind.utility.CaptchaSolver;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.executeJavaScript;

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

    @Resource
    private RecaptchaSolverService recaptchaSolverService;

    @SneakyThrows({IOException.class, InterruptedException.class})
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    public LinkedHashMap<String, String> carPrice(String regNr) {
        log.info("Searching car price for regNr: {}", regNr);
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
        String[] split = response.split(":");
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put(split[0], split[1] + "\n");
        result.put("Reg nr", regNr);
        log.info("Price for regNr: {} is {}", regNr, response);
        return result;
    }

    @SneakyThrows({InterruptedException.class})
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
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

    private boolean isAcceptedKey(String key) {
        return ACCEPTED_KEYS.stream().anyMatch(key::contains);
    }

    @Override
    public String getCaptchaToken() {
        log.info("Solving auto24 captcha");
        String token = recaptchaSolverService.solveCaptcha(SITE_KEY, PAGE_URL);
        log.info("Auto24 captcha solved");
        return token;
    }
}
