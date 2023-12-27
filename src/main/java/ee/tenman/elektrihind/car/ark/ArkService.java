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

import java.time.Duration;
import java.util.Arrays;
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
    private static final String AUTO_MAKS_URL = "https://www.err.ee/1609128527/uuendatud-kalkulaator-vaata-kui-suur-tuleb-sinu-automaks";

    @Resource(name = "fourThreadExecutor")
    private ExecutorService fourThreadExecutor;

    @Resource
    private TwoCaptchaSolverService recaptchaSolverService;

    @Override
    public String getCaptchaToken() {
        log.info("Solving ark captcha");
        String token = recaptchaSolverService.solveCaptcha(SITE_KEY, PAGE_URL);
        log.info("Ark captcha solved");
        return token;
    }

    private Optional<String> extractCarDetail(ElementsCollection elements, String conditionText) {
        return Optional.of(elements.find(Condition.text(conditionText)))
                .map(s -> s.sibling(0))
                .map(SelenideElement::text)
                .map(s -> s.replace("-", ""))
                .filter(StringUtils::isNotBlank);
    }

    private void parseKeyValuePairs(Map<String, String> carDetails, String input) {
        String[] lines = input.split("\n");
        for (int i = 0; i < lines.length; i += 2) {
            carDetails.put(lines[i], lines[i + 1]);
        }
    }

    private Map<String, String> getAutoMaks(Map<String, String> carDetails, String regNr) {
        log.info("Getting automaks for {} - {}", carDetails.get("Mark"), regNr);

        if (!"sõiduauto".equalsIgnoreCase(carDetails.get("Kategooria"))) {
            log.warn("Skipping. Car is not sõiduauto: {}", carDetails);
            return carDetails;
        }

        Selenide.open(AUTO_MAKS_URL);

        log.info("Filling in information for {} - {}", carDetails.get("Mark"), regNr);

        Optional<String> year = Optional.ofNullable(carDetails.get("Esmane registreerimine"))
                .map(s -> s.split("\\."))
                .stream()
                .flatMap(Arrays::stream)
                .filter(s -> s.matches("\\d{4}"))
                .findFirst();

        if (year.isEmpty()) {
            log.warn("Could not find year from car details: {}", carDetails);
            return carDetails;
        }

        Optional<String> taismass = Optional.ofNullable(carDetails.get("Täismass"))
                .map(s -> s.split(" "))
                .stream()
                .flatMap(Arrays::stream)
                .map(s -> s.replaceAll("\\D", ""))
                .findFirst();

        if (taismass.isEmpty()) {
            log.warn("Could not find taismass from car details: {}", carDetails);
            return carDetails;
        }

        Optional<String> co2 = Optional.ofNullable(carDetails.get("CO2 (NEDC)"))
                .map(s -> s.replaceAll("\\D", ""))
                .or(() -> Optional.ofNullable(carDetails.get("CO2 (WLTP)"))
                        .map(s -> s.replaceAll("\\D", "")));

        if (co2.isEmpty()) {
            Selenide.$$(By.tagName("label")).find(Condition.text("puudub")).click();

            Optional.ofNullable(carDetails.get("Tühimass"))
                    .map(s -> s.split(" "))
                    .stream()
                    .flatMap(Arrays::stream)
                    .map(s -> s.replaceAll("\\D", ""))
                    .findFirst()
                    .ifPresent(s -> Selenide.$(By.name("empty-mass")).setValue(s));

            Optional.ofNullable(carDetails.get("Mootori võimsus"))
                    .map(s -> s.split(" "))
                    .stream()
                    .flatMap(Arrays::stream)
                    .map(s -> s.replaceAll("\\D", ""))
                    .findFirst()
                    .ifPresent(s -> Selenide.$(By.name("vehicle-enginekW")).setValue(s));

            if (carDetails.containsKey("Kütus")) {
                boolean isBensiinFuel = carDetails.get("Kütus").toLowerCase().contains("bensiin");
                if (!isBensiinFuel) {
                    Selenide.$$(By.tagName("label")).find(Condition.text("diisel")).click();
                }
            }

        } else if (carDetails.containsKey("CO2 (NEDC)")) {
            Selenide.$$(By.tagName("label")).find(Condition.text("NEDC")).click();
        }

        Selenide.$(By.name("register-year")).setValue(year.get());
        Selenide.$(By.name("vehicle-mass")).setValue(taismass.get());
        co2.ifPresent(s -> Selenide.$(By.name("co2-value")).setValue(s));
        Selenide.$(By.className("tax-submit")).click();

        log.info("Retrieving information for automaks for {} - {}", carDetails.get("Mark"), regNr);

        Selenide.sleep(4000);

        log.info("Retrieved information for automaks for {} - {}", carDetails.get("Mark"), regNr);

        ElementsCollection divs = Selenide.$$(By.tagName("div"));

        SelenideElement aastamaks = divs.filter(Condition.text("Aastamaks"))
                .filter(Condition.text("Registreerimistasu"))
                .last();
        if (aastamaks.exists()) {
            Optional.of(aastamaks).map(SelenideElement::text)
                    .ifPresent(s -> parseKeyValuePairs(carDetails, s));
        }

        log.info("Found information for automaks for {} - {}", carDetails.get("Mark"), regNr);

        return carDetails;
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

        getAutoMaks(carDetails, regNr);

        log.info("Found car details for regNr: {}", regNr);

        fourThreadExecutor.submit(Selenide::closeWindow);
        return carDetails;
    }

}
