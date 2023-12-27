package ee.tenman.elektrihind.car.ark;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import ee.tenman.elektrihind.twocaptcha.TwoCaptchaSolverService;
import ee.tenman.elektrihind.utility.CaptchaSolver;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static ee.tenman.elektrihind.config.RedisConfig.ONE_MONTH_CACHE_5;

@Service
@Slf4j
public class ArkService implements CaptchaSolver {

    private static final String SITE_KEY = "6LepmygUAAAAAJB-Oalk-YSrlPj1dilm95QRY66J";
    private static final String PAGE_URL = "https://eteenindus.mnt.ee/public/soidukTaustakontroll.jsf";

    @Resource(name = "fourThreadExecutor")
    private ExecutorService fourThreadExecutor;

    @Resource
    private TwoCaptchaSolverService recaptchaSolverService;

    public static void downloadImage(String imageUrl, String destinationFile) throws IOException {
        try (InputStream in = new BufferedInputStream(new URL(imageUrl).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(destinationFile)) {
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            // handle exception
            throw e;
        }
    }

    @Override
    public String getCaptchaToken() {
        log.info("Solving ark captcha");
        String token = recaptchaSolverService.solveCaptcha(SITE_KEY, PAGE_URL);
        log.info("Ark captcha solved");
        return token;
    }

    static Optional<String> extractCarDetail(ElementsCollection elements, String conditionText) {
        return Optional.of(elements.find(Condition.text(conditionText)))
                .map(s -> s.sibling(0))
                .map(SelenideElement::text)
                .map(s -> s.replace("-", ""))
                .filter(StringUtils::isNotBlank);
    }

    @SneakyThrows
    @Cacheable(value = ONE_MONTH_CACHE_5, key = "#regNr")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public Map<String, String> carDetails(String regNr, String captchaToken) {
        if (StringUtils.isBlank(captchaToken)) {
            throw new RuntimeException("Captcha token is blank");
        }
        log.info("Searching car details for regNr: {}", regNr);
        Selenide.open(PAGE_URL);
        getWebDriver().manage().window().maximize();

        ElementsCollection spanCollection = $$(By.tagName("span"));

        spanCollection.shouldBe(CollectionCondition.sizeGreaterThan(1), Duration.ofSeconds(2));

        spanCollection.find(Condition.text("Registreerimismärk"))
                .parent() // Move to the parent td of the span
                .sibling(0) // Move to the next td sibling
                .$("input") // Find the input within this td
                .setValue(regNr);
        executeJavaScript("document.getElementById('g-recaptcha-response').innerHTML = arguments[0];", captchaToken);
        $$(By.tagName("button")).find(Condition.text("OTSIN")).click();
        SelenideElement contentTitle = Selenide.$(By.className("content-title"));
        contentTitle.shouldBe(Condition.exist, Duration.ofSeconds(3));
        if (!contentTitle.exists()) {
            log.info("No car found for regNr: {}", regNr);
            return new LinkedHashMap<>();
        }

        ElementsCollection titles = contentTitle.findAll(By.tagName("p"));
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


        SelenideElement logoElement = Selenide.$(By.className("asset-image"));
        String logo = null;
        if (logoElement.exists()) {
            SelenideElement imageElement = logoElement.find(By.tagName("img"));
            if (imageElement.exists()) {
                logo = imageElement.attr("src");
            }
        }

        ElementsCollection rows = Selenide.$(By.className("asset-details")).findAll(By.tagName("tr"));
        Map<String, String> carDetails = new LinkedHashMap<>();

        carDetails.put("Mark", carName);
        carDetails.put("Vin", vin);
        if (logo != null) {
            carDetails.put("Logo", logo);
        }
        for (int i = 0; i < rows.size(); i++) {
            ElementsCollection td = rows.get(i).$$("td");
            String key = td.get(0).getText().replace(":", "");
            String value = td.get(1).getText();
            carDetails.put(key, value);
        }

        ElementsCollection carTitles = $$(By.className("title"));

        extractCarDetail(carTitles, "CO2 (NEDC)").ifPresent(s -> carDetails.put("CO2 (NEDC)", s));
        extractCarDetail(carTitles, "CO2 (WLTP)").ifPresent(s -> carDetails.put("CO2 (WLTP)", s));
        extractCarDetail(carTitles, "Täismass").ifPresent(s -> carDetails.put("Täismass", s));
        extractCarDetail(carTitles, "Tühimass").ifPresent(s -> carDetails.put("Tühimass", s));

        log.info("Found car details for regNr: {}", regNr);
        fourThreadExecutor.submit(Selenide::closeWindow);
        return carDetails;
    }

}
