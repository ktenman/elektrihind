package ee.tenman.elektrihind.car.lkf;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import ee.tenman.elektrihind.twocaptcha.TwoCaptchaSolverService;
import ee.tenman.elektrihind.utility.CaptchaSolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static ee.tenman.elektrihind.config.RedisConfiguration.ONE_MONTH_CACHE_1;

@Service
@Slf4j
public class LKFService implements CaptchaSolver {

    private static final String SITE_KEY = "6LcKDjIUAAAAALL0hvZVDGSKpVVeumbgkeryE3DP";
    private static final String PAGE_URL = "https://lkf.ee/et/kahjukontroll";
    @Resource(name = "fourThreadExecutor")
    private ExecutorService fourThreadExecutor;

    @Resource
    private TwoCaptchaSolverService recaptchaSolverService;

    @Cacheable(value = ONE_MONTH_CACHE_1, key = "#regNr")
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000))
    public Map<String, String> carDetails(String regNr, String captchaToken) {
        log.info("Searching lkf car details for regNr: {}", regNr);
        Selenide.open(PAGE_URL);
        getWebDriver().manage().window().maximize();
        Selenide.$(By.name("vehicle")).setValue(regNr);
        executeJavaScript("document.getElementById('g-recaptcha-response').innerHTML = arguments[0];", captchaToken);
        Selenide.$(By.id("edit-submit")).shouldBe(Condition.visible).click();
        Selenide.sleep(TimeUnit.SECONDS.toMillis(2));
        ElementsCollection elements = $$(By.tagName("p"));
        boolean hadCrashes = elements.find(Condition.text("on osalenud kindlustusjuhtumites")).exists() ||
                elements.find(Condition.text("on osalenud kindlustusjuhtumis")).exists();

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
