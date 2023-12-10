package ee.tenman.elektrihind.car.scrapeninja;

import ee.tenman.elektrihind.IntegrationTest;
import ee.tenman.elektrihind.car.auto24.Auto24Service;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

@IntegrationTest
class ScrapeninjaServiceTest {

    @Resource
    ScrapeninjaService scrapeninjaService;

    @Resource
    Auto24Service auto24Service;

    @Test
    @Disabled
    void scrape() {
        String captchaToken = auto24Service.getCaptchaToken();
        Map<String, String> scraped = scrapeninjaService.scrape("JF1SH5LS5AG105986", "876BCH", captchaToken);

        System.out.println();
    }
}
