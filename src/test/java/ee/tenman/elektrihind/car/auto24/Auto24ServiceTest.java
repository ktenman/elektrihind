package ee.tenman.elektrihind.car.auto24;

import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

@IntegrationTest
class Auto24ServiceTest {

    @Resource
    Auto24Service auto24Service;

    @Test
    @Disabled
    void carDetails() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("Reg nr", "999ilo");
        map.put("Vin", "WAUZZZF20KN004417");
        String captchaToken = auto24Service.getCaptchaToken();
        Map<String, String> stringStringMap = auto24Service.carDetails(map, captchaToken);

        System.out.println();
    }

    @Test
    @Disabled
    void carPrice() {
        LinkedHashMap<String, String> map = auto24Service.carPrice("876BCH");

        System.out.println();
    }
}
