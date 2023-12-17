package ee.tenman.elektrihind.car;

import com.codeborne.selenide.Configuration;
import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

@IntegrationTest
class CarSearchServiceTest {

    @Resource
    private CarSearchService carSearchService;

    @Test
    @Disabled
    void search() {
        long start = System.nanoTime();

        Map<String, String> search = carSearchService.search("876BCH");

        long end = System.nanoTime();
        double duration = (end - start) / 1_000_000_000.0;
        System.out.println("Duration: " + duration);
    }

    @Test
    @Disabled
    void search2() {
//        Configuration.browser = "chrome";
        Configuration.headless = false;
        long start = System.nanoTime();
        Map<String, String> search = carSearchService.search2("57DV");

        long end = System.nanoTime();
        double duration = (end - start) / 1_000_000_000.0;
        System.out.println("Duration: " + duration);
    }
}
