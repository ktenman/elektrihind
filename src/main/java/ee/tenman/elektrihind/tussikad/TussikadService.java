package ee.tenman.elektrihind.tussikad;

import com.codeborne.selenide.Selenide;
import org.openqa.selenium.By;
import org.springframework.stereotype.Service;

@Service
public class TussikadService {

    private String username = "refineoutflow@protonmail.com";
    private String password = "cheesy-boiler-mandolin-squat";
    private String url = "https://www.patreon.com/login?ru=%2Ftussisoojad";

    public void getYoutubeUrls() {
        Selenide.open(url);
        Selenide.sleep(3000);
        Selenide.$(By.name("email")).setValue(username);
        Selenide.$$(By.tagName("button")).last().click();
        Selenide.$(By.name("current-password")).setValue(password);
        Selenide.$$(By.tagName("button")).last().click();
        System.out.println();

    }

}
