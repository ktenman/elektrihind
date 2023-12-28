package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
import org.assertj.core.api.Assertions;
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
        long start = System.nanoTime();
        Map<String, String> search = carSearchService.search2("011BKM");

        long end = System.nanoTime();
        double duration = (end - start) / 1_000_000_000.0;
        System.out.println("Duration: " + duration);

        Assertions.assertThat(search).hasSize(18);
    }
}
