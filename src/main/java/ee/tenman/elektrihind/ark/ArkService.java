package ee.tenman.elektrihind.ark;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import ee.tenman.elektrihind.auto24.RecaptchaSolverService;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static ee.tenman.elektrihind.config.RedisConfig.ONE_YEAR_CACHE_1;

@Service
@Slf4j
public class ArkService {

    private static final String SITE_KEY = "6LepmygUAAAAAJB-Oalk-YSrlPj1dilm95QRY66J";
    private static final String PAGE_URL = "https://eteenindus.mnt.ee/public/soidukTaustakontroll.jsf";

    @Resource
    private RecaptchaSolverService recaptchaSolverService;

    @SneakyThrows({InterruptedException.class})
    @Cacheable(value = ONE_YEAR_CACHE_1, key = "#regNr")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public Map<String, String> carDetails(String regNr) {

        Selenide.open(PAGE_URL);
        getWebDriver().manage().window().maximize();
        TimeUnit.SECONDS.sleep(1);

        ElementsCollection spanCollection = $$(By.tagName("span"));
        spanCollection.find(Condition.text("Registreerimismärk"))
                .parent() // Move to the parent td of the span
                .sibling(0) // Move to the next td sibling
                .$("input") // Find the input within this td
                .setValue(regNr);

        log.info("Solving car captcha for regNr: {}", regNr);
        String captchaToken = recaptchaSolverService.solveCaptcha(SITE_KEY, PAGE_URL);
        log.info("Car captcha solved for regNr: {}", regNr);
        executeJavaScript("document.getElementById('g-recaptcha-response').innerHTML = arguments[0];", captchaToken);
        $$(By.tagName("button")).find(Condition.text("OTSIN")).click();
        ElementsCollection titles = Selenide.$(By.className("content-title")).findAll(By.tagName("p"));
        String carName = Optional.of(titles)
                .map(ElementsCollection::first)
                .map(SelenideElement::text)
                .orElseThrow(() -> new RuntimeException("Car name not found"));
        String vin = Optional.of(titles)
                .map(ElementsCollection::last)
                .map(SelenideElement::text)
                .map(text -> text.split("VIN: "))
                .map(strings -> strings[1])
                .orElseThrow(() -> new RuntimeException("VIN not found"));

        TimeUnit.SECONDS.sleep(3);

        ElementsCollection rows = Selenide.$(By.className("asset-details")).findAll(By.tagName("tr"));
        Map<String, String> carDetails = new LinkedHashMap<>();

        carDetails.put("Reg nr", regNr);
        carDetails.put("Mark", carName + "\n");
        carDetails.put("Vin", vin + "\n");
        for (int i = 0; i < rows.size(); i++) {
            ElementsCollection td = rows.get(i).$$("td");
            String key = td.get(0).getText().replace(":", "");
            String value = td.get(1).getText();
            carDetails.put(key, value);
        }
        Selenide.closeWindow();
        log.info("Found car details for regNr: {}", regNr);
        return carDetails;
    }

}
