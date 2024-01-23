package ee.tenman.elektrihind.car.ark;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import ee.tenman.elektrihind.cache.CacheService;
import ee.tenman.elektrihind.car.automaks.AutomaksService;
import ee.tenman.elektrihind.car.automaks.CarDetails;
import ee.tenman.elektrihind.car.automaks.TaxResponse;
import ee.tenman.elektrihind.electricity.CarSearchUpdateListener;
import ee.tenman.elektrihind.twocaptcha.TwoCaptchaSolverService;
import ee.tenman.elektrihind.utility.CaptchaSolver;
import ee.tenman.elektrihind.utility.PDFDataExtractor;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static ee.tenman.elektrihind.config.RedisConfiguration.ONE_MONTH_CACHE_5;
import static java.util.Comparator.comparingLong;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.tagName;

@Service
@Slf4j
public class ArkService implements CaptchaSolver {

    private static final String SITE_KEY = "6LepmygUAAAAAJB-Oalk-YSrlPj1dilm95QRY66J";
    private static final String PAGE_URL = "https://eteenindus.mnt.ee/public/soidukTaustakontroll.jsf";

    @Resource
    private TwoCaptchaSolverService recaptchaSolverService;

    @Resource
    private AutomaksService automaksService;
    
    @Resource
    private CacheService cacheService;

    @Override
    public String getCaptchaToken() {
        log.info("Solving ark captcha");
        if (!cacheService.isArkCaptchaDetectionEnabled()) {
            log.info("Ark captcha detection disabled");
            return "";
        }
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

    private static void addMarkAndVin(Map<String, String> carDetails, SelenideElement contentTitle) {
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

        ElementsCollection rows = Selenide.$(className("asset-details")).findAll(tagName("tr"));

        carDetails.put("Mark", carName + "\n");
        carDetails.put("Vin", vin + "\n");
        for (int i = 0; i < rows.size(); i++) {
            ElementsCollection td = rows.get(i).$$("td");
            String key = td.get(0).getText().replace(":", "");
            String value = td.get(1).getText();
            carDetails.put(key, value);
        }
    }

    private static void addLatestOdometerFromPDF(Map<String, String> carDetails) {
        ElementsCollection elements = $$(tagName("a")).filter(text("Salvestan"));
        if (elements.isEmpty() || elements.size() == 1) {
            return;
        }
        elements.last().click();
        File downloadsMainFolder = new File(Configuration.downloadsFolder);
        Optional<File> latestPDF = Stream.of(Optional.ofNullable(downloadsMainFolder.listFiles(File::isDirectory))
                        .orElse(new File[0]))
                .max(comparingLong(File::lastModified))
                .flatMap(latestDirectory -> Stream.of(Optional.ofNullable(latestDirectory.listFiles(
                                (dir, name) -> name.toLowerCase().endsWith(".pdf"))).orElse(new File[0]))
                        .max(comparingLong(File::lastModified)));
        latestPDF.ifPresent(pdf -> {
            Map<String, Integer> odometerAndDate = PDFDataExtractor.extractDateAndOdometer(pdf.getAbsolutePath());
            Optional<Entry<String, Integer>> maxOdometerEntry = PDFDataExtractor.getLastOdometer(odometerAndDate);
            maxOdometerEntry.ifPresent(entry -> carDetails.put("Läbisõit", entry.getValue() + " (" + entry.getKey() + ")"));
        });
    }

    @SneakyThrows
    @Cacheable(value = ONE_MONTH_CACHE_5, key = "#regNr")
    public Map<String, String> carDetails(
            String regNr,
            String captchaToken,
            Map<String, String> carData,
            CarSearchUpdateListener updateListener
    ) {
        if (StringUtils.isBlank(captchaToken) && cacheService.isArkCaptchaDetectionEnabled()) {
            throw new RuntimeException("Captcha token is blank");
        }
        log.info("Searching car details for regNr: {}", regNr);
        open(PAGE_URL);
        getWebDriver().manage().window().maximize();

        ElementsCollection spanCollection = $$(tagName("span"));
        spanCollection.shouldBe(CollectionCondition.sizeGreaterThan(1), Duration.ofSeconds(2));
        spanCollection.find(text("Registreerimismärk"))
                .parent()
                .sibling(0)
                .$("input")
                .setValue(regNr);
        if (cacheService.isArkCaptchaDetectionEnabled()) {
            executeJavaScript("document.getElementById('g-recaptcha-response').innerHTML = arguments[0];", captchaToken);
        }
        $$(tagName("button")).find(text("OTSIN")).click();
        TimeUnit.SECONDS.sleep(3);
        SelenideElement contentTitle = Selenide.$(className("content-title"));
        if (!contentTitle.exists()) {
            log.info("No car found for regNr: {}", regNr);
            return new LinkedHashMap<>();
        }

        addLatestOdometerFromPDF(carData);
        addMarkAndVin(carData, contentTitle);
        updateListener.onUpdate(carData, false);

        ElementsCollection carTitles = $$(className("title"));
        extractCarDetail(carTitles, "CO2 (NEDC)").ifPresent(s -> carData.put("CO2 (NEDC)", s));
        extractCarDetail(carTitles, "CO2 (WLTP)").ifPresent(s -> carData.put("CO2 (WLTP)", s));
        extractCarDetail(carTitles, "Täismass").ifPresent(s -> carData.put("Täismass", s));
        extractCarDetail(carTitles, "Tühimass").ifPresent(s -> carData.put("Tühimass", s));
        UnaryOperator<String> stringModifier = s -> s.replace(" l/100km", "");
        extractCarDetail(carTitles, "Keskmine (NEDC)")
                .or(() -> extractCarDetail(carTitles, "Keskmine (WLTP)"))
                .ifPresent(s -> carData.put("Kütusekulu keskmine", stringModifier.apply(s)));
        extractCarDetail(carTitles, "Linnas").ifPresent(s -> carData.put("Kütusekulu linnas", stringModifier.apply(s)));
        extractCarDetail(carTitles, "Maanteel").ifPresent(s -> carData.put("Kütusekulu maanteel", stringModifier.apply(s)));
        extractCarDetail(carTitles, "Kategooria tähis").ifPresent(s -> carData.put("Kategooria tähis", s));

        CarDetails carDetails = new CarDetails(carData);
        Optional<TaxResponse> calculatedTax = automaksService.calculate(carDetails);
        calculatedTax.ifPresent(
                taxResponse -> {
                    carData.put("Registreerimismaks", taxResponse.getRegistrationTax().toString());
                    carData.put("Aastamaks", taxResponse.getAnnualTax().toString());
                    updateListener.onUpdate(carData, true);
                }
        );

        log.info("Found car details for regNr: {}", regNr);
        Selenide.closeWindow();
        return carData;
    }

}
