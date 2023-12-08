package ee.tenman.elektrihind.euribor;

import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@IntegrationTest
class EuriborRateFetcherTest {

    @Resource
    private EuriborRateFetcher euriborRateFetcher;

    @Test
    @Disabled
    void getLastKnownEuriborRate() {
        String r = euriborRateFetcher.getEuriborRateResponse();

        System.out.println();
    }
}
