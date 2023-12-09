package ee.tenman.elektrihind.auto24;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static ee.tenman.elektrihind.config.RedisConfig.ONE_DAY_CACHE;

@Service
@Slf4j
public class Auto24Service {

//    Reg nr: 876BCH
//    Mark: SUBARU FORESTER
//
//    Vin: JF1SH5LS5AG105986
//
//    Esmane registreerimine: 18.09.2009
//    Kategooria: sõiduauto
//    Kere nimetus: universaal
//    Kere värvus: helehall
//    Mootor: 1994 cm³
//    Mootori võimsus: 110 kW
//    Kütus: Mootoribensiin
//    Käigukast: Automaat
//    Veoskeem: nelikvedu
//    Registreerimistunnistus: EL813202
//    Kütusekulu keskmine: 8.4
//    Kütusekulu linnas: 11.2
//    Kütusekulu maanteel: 6.9
//    Läbisõit: 120141 (31.07.2023)

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

    @SneakyThrows
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    @Cacheable(value = ONE_DAY_CACHE, key = "#regNr")
    public String search(String regNr) {

        Map<String, String> details = carDetails(regNr);
        String price = carPrice(regNr);

        return price + "\n\n" + details.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

}
