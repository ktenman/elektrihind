package ee.tenman.elektrihind.car.automaks;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class AutoMaksService {

    private static final String AUTO_MAKS_URL = "https://www.err.ee/1609128527/uuendatud-kalkulaator-vaata-kui-suur-tuleb-sinu-automaks";

    private static void parseKeyValuePairs(Map<String, String> carDetails, String input) {
        String[] lines = input.split("\n");
        for (int i = 0; i < lines.length; i += 2) {
            carDetails.put(lines[i], lines[i + 1]);
        }
    }

    public Map<String, String> getAutoMaks(Map<String, String> carDetails) {
        log.info("Getting automaks");

        if (!"sõiduauto".equalsIgnoreCase(carDetails.get("Kategooria"))) {
            log.warn("Skipping. Car is not sõiduauto: {}", carDetails);
            return carDetails;
        }

        Selenide.open(AUTO_MAKS_URL);

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

        Selenide.sleep(5000);

        ElementsCollection divs = Selenide.$$(By.tagName("div"));

        SelenideElement aastamaks = divs
                .filter(Condition.text("Aastamaks"))
                .filter(Condition.text("Registreerimistasu"))
                .last();
        if (aastamaks.exists()) {
            Optional.of(aastamaks).map(SelenideElement::text)
                    .ifPresent(s -> parseKeyValuePairs(carDetails, s));
        }

        return carDetails;
    }

}
