package ee.tenman.elektrihind.auto24;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
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

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.executeJavaScript;

@Service
@Slf4j
public class Auto24Service {

    static {
        Configuration.headless = true;
        Configuration.browser = "firefox";
    }

    @Resource
    private RecaptchaSolverService recaptchaSolverService;

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    public Map<String, String> carDetails(String regNr) {
        Selenide.open("https://www.auto24.ee/ostuabi/?t=soiduki-andmete-paring");
        SelenideElement acceptCookies = $(By.id("onetrust-accept-btn-handler"));
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
//        Selenide.closeWebDriver();
        return carDetails;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    @SneakyThrows(IOException.class)
    public String carPrice(String regNr) {
        Selenide.open("https://www.auto24.ee/ostuabi/?t=soiduki-turuhinna-paring");
        SelenideElement acceptCookies = $(By.id("onetrust-accept-btn-handler"));
        if (acceptCookies.exists()) {
            acceptCookies.click();
        }
        $(By.name("vpc_reg_nr")).setValue(regNr);

        File screenshot = $("#vpc_captcha").screenshot();
        assert screenshot != null;
        log.info("Solving price captcha for regNr: {}", regNr);
        String solveCaptcha = recaptchaSolverService.solveCaptcha(Files.readAllBytes(screenshot.toPath()));
        $(By.name("checksec1")).setValue(solveCaptcha);
        log.info("Price Captcha solved for regNr: {}", regNr);
        $("button[type='submit']").click();
        int count = 0;
        while ($(".errorMessage").exists() && count++ < 5) {
            log.warn("Invalid captcha for regNr: {}", regNr);
            screenshot = $("#vpc_captcha").screenshot();
            assert screenshot != null;
            log.info("Solving captcha for regNr: {}", regNr);
            solveCaptcha = recaptchaSolverService.solveCaptcha(Files.readAllBytes(screenshot.toPath()));
            $(By.name("checksec1")).setValue(solveCaptcha);
            log.info("Captcha solved for regNr: {}", regNr);
            $("button[type='submit']").click();
        }
        String response = $$(By.tagName("div")).filter(Condition.text("Sõiduki keskmine hind")).last().parent().text();
//        Selenide.closeWebDriver();
        return response;
    }
}
