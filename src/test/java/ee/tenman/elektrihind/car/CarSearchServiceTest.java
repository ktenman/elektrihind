package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@IntegrationTest
class CarSearchServiceTest {

    @Resource
    private CarSearchService carSearchService;

    @Test
    @Disabled
    void search() {
        String search = carSearchService.search("876BCH");

        System.out.println();
    }
}
