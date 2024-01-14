package ee.tenman.elektrihind.car.ark;

import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

@IntegrationTest
class ArkServiceTest {

    @Resource
    ArkService arkService;

    @Test
    @Disabled
    void carDetails() {
        String captchaToken = arkService.getCaptchaToken();
        Map<String, String> stringStringMap = arkService.carDetails("876BCH", captchaToken, new HashMap<>(), (
                carDetails, isFinalUpdate) -> System.out.println(carDetails));
    }
}
