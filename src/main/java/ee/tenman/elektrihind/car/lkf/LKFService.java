package ee.tenman.elektrihind.car.lkf;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import ee.tenman.elektrihind.recaptcha.RecaptchaSolverService;
import ee.tenman.elektrihind.utility.CaptchaSolver;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static ee.tenman.elektrihind.config.RedisConfig.ONE_DAY_CACHE_1;

@Service
@Slf4j
public class LKFService implements CaptchaSolver {

    private static final String SITE_KEY = "6LcKDjIUAAAAALL0hvZVDGSKpVVeumbgkeryE3DP";
    private static final String PAGE_URL = "https://lkf.ee/et/kahjukontroll";

    @Resource
    private RecaptchaSolverService recaptchaSolverService;

    @SneakyThrows({InterruptedException.class})
    @Cacheable(value = ONE_DAY_CACHE_1, key = "#regNr")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public Map<String, String> carDetails(String regNr, String captchaToken) {
        log.info("Searching lkf car details for regNr: {}", regNr);
        Selenide.open(PAGE_URL);
        getWebDriver().manage().window().maximize();
        TimeUnit.SECONDS.sleep(1);
        Selenide.$(By.name("vehicle")).setValue(regNr);
        executeJavaScript("document.getElementById('g-recaptcha-response').innerHTML = arguments[0];", captchaToken);
        Selenide.$(By.id("edit-submit")).click();
        TimeUnit.SECONDS.sleep(2);

        boolean hadCrashes = Selenide.$$(By.tagName("p")).find(Condition.text("on osalenud kindlustusjuhtumites")).exists();

        if (!hadCrashes) {
            log.info("No crashes found for regNr: {}", regNr);
            return new HashMap<>();
        }

        int crashes = $$(By.tagName("caption")).texts().size();
        log.info("Found lkf car crashes for regNr: {}", regNr);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("Avariide arv", String.valueOf(crashes));
        Selenide.closeWindow();
        return result;
    }

    @Override
    public String getCaptchaToken() {
        log.info("Solving lkf captcha");
        String token = recaptchaSolverService.solveCaptcha(SITE_KEY, PAGE_URL);
        log.info("Lkf captcha solved");
        return token;
    }
}
