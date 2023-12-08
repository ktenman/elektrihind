package ee.tenman.elektrihind.auto24;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.springframework.stereotype.Service;

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
        Configuration.browser = "chrome";
    }

    @Resource
    private RecaptchaSolverService recaptchaSolverService;

    public Map<String, String> carDetails(String regNr) {
        Selenide.open("https://www.auto24.ee/ostuabi/?t=soiduki-andmete-paring");
        $(By.id("onetrust-accept-btn-handler")).click();
        $(By.id("reg_nr-inp")).setValue(regNr);
        log.info("Solving captcha for regNr: {}", regNr);
        String captchaToken = recaptchaSolverService.solveCaptcha();
        log.info("Captcha solved for regNr: {}", regNr);
        executeJavaScript("document.getElementById('g-recaptcha-response').innerHTML = arguments[0];", captchaToken);
        $("button[type='submit']").click();
        ElementsCollection rows = $$("table.result tr");
        Map<String, String> carDetails = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            String key = rows.get(i).$$("td").get(0).getText();
            String value = rows.get(i).$$("td").get(1).getText();
            carDetails.put(key, value);
        }
        Selenide.closeWebDriver();
        return carDetails;

    }
}
