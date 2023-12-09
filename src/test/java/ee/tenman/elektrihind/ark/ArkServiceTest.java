package ee.tenman.elektrihind.ark;

import ee.tenman.elektrihind.IntegrationTest;
import ee.tenman.elektrihind.auto24.Auto24Service;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@IntegrationTest
class ArkServiceTest {

    @Resource
    private ArkService arkService;

    @Resource
    private Auto24Service auto24Service;

    @Test
    @Disabled
    void carDetails() {
        String details = auto24Service.search("876BCH");

        System.out.println();
    }
}
