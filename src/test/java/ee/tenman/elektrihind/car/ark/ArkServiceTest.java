package ee.tenman.elektrihind.car.ark;

import com.codeborne.selenide.Configuration;
import ee.tenman.elektrihind.IntegrationTest;
import ee.tenman.elektrihind.cache.CacheService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ArkServiceTest {

    @Resource
    ArkService arkService;
    
    @Resource
    CacheService cacheService;

    @Test
    @Disabled
    void carDetails() {
        String captchaToken = arkService.getCaptchaToken();
        Configuration.headless = false;
        Map<String, String> result = arkService.carDetails("205HKH", captchaToken, new HashMap<>(), (
                carDetails, isFinalUpdate) -> System.out.println(carDetails));
        
        assertThat(result).isNotEmpty().hasSize(17);
    }
}
