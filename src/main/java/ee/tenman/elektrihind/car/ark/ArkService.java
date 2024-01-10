package ee.tenman.elektrihind.car.ark;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import ee.tenman.elektrihind.cache.CacheService;
import ee.tenman.elektrihind.electricity.CarSearchUpdateListener;
import ee.tenman.elektrihind.twocaptcha.TwoCaptchaSolverService;
import ee.tenman.elektrihind.utility.CaptchaSolver;
import ee.tenman.elektrihind.utility.PDFDataExtractor;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static ee.tenman.elektrihind.config.RedisConfiguration.ONE_MONTH_CACHE_5;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.tagName;

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

    @Resource
    private CacheService cacheService;

    @Override
    public String getCaptchaToken() {
        log.info("Solving ark captcha");
        String token = recaptchaSolverService.solveCaptcha(SITE_KEY, PAGE_URL);
        log.info("Ark captcha solved");
        return token;
    }

    private Optional<String> extractCarDetail(ElementsCollection elements, String conditionText) {
        try {
            SelenideElement selenideElement = elements.find(text(conditionText));
            if (!selenideElement.exists()) {
                return Optional.empty();
            }
            return Optional.of(selenideElement)
                    .map(s -> s.sibling(0))
                    .map(SelenideElement::text)
                    .map(s -> s.replace("-", ""))
                    .filter(StringUtils::isNotBlank);
        } catch (Exception e) {
            log.error("Error extracting car detail", e);
        }
        return Optional.empty();
    }

    private void parseKeyValuePairs(Map<String, String> carDetails, String input) {
        String[] lines = input.split("\n");
        for (int i = 0; i < lines.length; i += 2) {
            carDetails.put(lines[i], lines[i + 1]);
        }
    }

    private Map<String, String> getAutoMaks(Map<String, String> carDetails, String regNr) {
        if (!StringUtils.containsIgnoreCase(carDetails.get("Kategooria"), "sõiduauto")) {
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
                .map(this::extractNumericValue);

        if (taismass.isEmpty()) {
            log.warn("Could not find taismass from car details: {}", carDetails);
            return carDetails;
        }

        Optional<String> co2 = Optional.ofNullable(carDetails.get("CO2 (WLTP)"))
                .map(this::extractNumericValue)
                .or(() -> Optional.ofNullable(carDetails.get("CO2 (NEDC)")).map(this::extractNumericValue));

        if (co2.isEmpty()) {
            $$(tagName("label")).find(text("puudub")).click();

            Optional.ofNullable(carDetails.get("Tühimass"))
                    .map(this::extractNumericValue)
                    .ifPresent(s -> Selenide.$(By.name("empty-mass")).setValue(s));

            Optional.ofNullable(carDetails.get("Mootori võimsus"))
                    .map(this::extractNumericValue)
                    .ifPresent(s -> Selenide.$(By.name("vehicle-enginekW")).setValue(s));

            if (carDetails.containsKey("Kütus")) {
                boolean isBensiinFuel = carDetails.get("Kütus").toLowerCase().contains("bensiin");
                if (!isBensiinFuel) {
                    $$(tagName("label")).find(text("diisel")).click();
                }
            }
        } else if (carDetails.containsKey("CO2 (WLTP)")) {
            log.info("Leaving default as is. CO2 (WLTP) for {} - {}", carDetails.get("Mark"), regNr);
        } else if (carDetails.containsKey("CO2 (NEDC)")) {
            log.info("Selecting. Found CO2 (NEDC) for {} - {}", carDetails.get("Mark"), regNr);
            $$(tagName("label")).find(text("NEDC")).click();
        }

        Selenide.$(By.name("register-year")).setValue(year.get());
        Selenide.$(By.name("vehicle-mass")).setValue(taismass.get());
        co2.ifPresent(s -> Selenide.$(By.name("co2-value")).setValue(s));
        Selenide.$(className("tax-submit"))
                .shouldBe(Condition.visible)
                .click();

        log.info("Retrieved information for automaks for {} - {}", carDetails.get("Mark"), regNr);

        SelenideElement aastamaks = Selenide.$(className("results-container")).shouldBe(Condition.visible);
        if (aastamaks.exists()) {
            Optional.of(aastamaks).map(SelenideElement::text)
                    .ifPresent(s -> parseKeyValuePairs(carDetails, s));
        }

        log.info("Found information for automaks for {} - {}", carDetails.get("Mark"), regNr);

        return carDetails;
    }

    private String extractNumericValue(String string) {
        if (string == null || !string.contains(" ")) {
            return null;
        }

        Pattern pattern = Pattern.compile("\\b\\d{2,4}\\b");
        Matcher matcher = pattern.matcher(string);
        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    @SneakyThrows
    @Cacheable(value = ONE_MONTH_CACHE_5, key = "#regNr")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public Map<String, String> carDetails(String regNr, String captchaToken, Map<String, String> carDetails, CarSearchUpdateListener updateListener) {
        if (StringUtils.isBlank(captchaToken)) {
            throw new RuntimeException("Captcha token is blank");
        }
        log.info("Searching car details for regNr: {}", regNr);
        Selenide.open(PAGE_URL);
        getWebDriver().manage().window().maximize();

        ElementsCollection spanCollection = $$(tagName("span"));

        spanCollection.shouldBe(CollectionCondition.sizeGreaterThan(1), Duration.ofSeconds(2));

        spanCollection.find(text("Registreerimismärk"))
                .parent() // Move to the parent td of the span
                .sibling(0) // Move to the next td sibling
                .$("input") // Find the input within this td
                .setValue(regNr);
        executeJavaScript("document.getElementById('g-recaptcha-response').innerHTML = arguments[0];", captchaToken);
        $$(tagName("button")).find(text("OTSIN")).click();
        TimeUnit.SECONDS.sleep(3);
        SelenideElement contentTitle = Selenide.$(className("content-title"));
        if (!contentTitle.exists()) {
            log.info("No car found for regNr: {}", regNr);
            return new LinkedHashMap<>();
        }
        $$(tagName("a")).filter(text("Salvestan")).last().click();
        File downloadsMainFolder = new File(Configuration.downloadsFolder);
        Optional<File> latestPDF = Stream.of(Optional.ofNullable(downloadsMainFolder.listFiles(File::isDirectory))
                        .orElse(new File[0]))
                .max(Comparator.comparingLong(File::lastModified))
                .flatMap(latestDirectory -> Stream.of(Optional.ofNullable(latestDirectory.listFiles(
                                (dir, name) -> name.toLowerCase().endsWith(".pdf"))).orElse(new File[0]))
                        .max(Comparator.comparingLong(File::lastModified)));

        latestPDF.ifPresent(pdf -> {
            Map<String, Integer> odometerAndDate = PDFDataExtractor.extractDateAndOdometer(pdf.getAbsolutePath());
            Optional<Entry<String, Integer>> maxOdometerEntry = PDFDataExtractor.getLastOdometer(odometerAndDate);
            maxOdometerEntry.ifPresent(entry -> carDetails.put("Läbisõit", entry.getValue() + " (" + entry.getKey() + ")"));
        });

        ElementsCollection titles = contentTitle.findAll(tagName("p"));
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

//        SelenideElement logoElement = Selenide.$(By.className("asset-image"));
//        String logo = null;
//        if (logoElement.exists()) {
//            SelenideElement imageElement = logoElement.find(By.tagName("img"));
//            if (imageElement.exists()) {
//                logo = imageElement.attr("src");
//            }
//        }

        ElementsCollection rows = Selenide.$(className("asset-details")).findAll(tagName("tr"));

        carDetails.put("Mark", carName + "\n");
        carDetails.put("Vin", vin + "\n");
//        if (logo != null) {
//            carDetails.put("Logo", logo);
//        }
        for (int i = 0; i < rows.size(); i++) {
            ElementsCollection td = rows.get(i).$$("td");
            String key = td.get(0).getText().replace(":", "");
            String value = td.get(1).getText();
            carDetails.put(key, value);
        }

        updateListener.onUpdate(carDetails, false);

        cacheService.setAutomaksEnabled(true);

        ElementsCollection carTitles = $$(className("title"));
        if (cacheService.isAutomaksEnabled()) {
            log.info("Getting automaks for {} - {}", carDetails.get("Mark"), regNr);
            extractCarDetail(carTitles, "CO2 (NEDC)").ifPresent(s -> carDetails.put("CO2 (NEDC)", s));
            extractCarDetail(carTitles, "CO2 (WLTP)").ifPresent(s -> carDetails.put("CO2 (WLTP)", s));
            extractCarDetail(carTitles, "Täismass").ifPresent(s -> carDetails.put("Täismass", s));
            extractCarDetail(carTitles, "Tühimass").ifPresent(s -> carDetails.put("Tühimass", s));
            UnaryOperator<String> stringModifier = s -> s.replace(" l/100km", "");
            extractCarDetail(carTitles, "Keskmine (NEDC)")
                    .or(() -> extractCarDetail(carTitles, "Keskmine (WLTP)"))
                    .ifPresent(s -> carDetails.put("Kütusekulu keskmine", stringModifier.apply(s)));
            extractCarDetail(carTitles, "Linnas").ifPresent(s -> carDetails.put("Kütusekulu linnas", stringModifier.apply(s)));
            extractCarDetail(carTitles, "Maanteel").ifPresent(s -> carDetails.put("Kütusekulu maanteel", stringModifier.apply(s)));
            getAutoMaks(carDetails, regNr);
        } else {
            log.info("Skipping automaks for {} - {}", carDetails.get("Mark"), regNr);
        }

        log.info("Found car details for regNr: {}", regNr);
        Selenide.closeWindow();
        return carDetails;
    }

    @SneakyThrows
    @Cacheable(value = ONE_MONTH_CACHE_5, key = "#regNr")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public Map<String, String> carDetails(String regNr, String captchaToken) {
        return carDetails(regNr, captchaToken, new LinkedHashMap<>(), (carDetails, isLast) -> {
        });
    }

}
