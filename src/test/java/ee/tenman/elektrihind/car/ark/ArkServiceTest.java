package ee.tenman.elektrihind.car.ark;

import com.codeborne.selenide.Configuration;
import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

@IntegrationTest
class ArkServiceTest {

    @Resource
    private ArkService arkService;

    @Test
    @Disabled
    void carDetails() {
        Configuration.headless = false;
        String captchaToken = arkService.getCaptchaToken();
        Map<String, String> details = arkService.carDetails("077LSK", captchaToken);

        System.out.println();
    }
}
