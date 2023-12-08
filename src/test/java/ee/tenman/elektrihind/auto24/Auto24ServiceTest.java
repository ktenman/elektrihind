package ee.tenman.elektrihind.auto24;

import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class Auto24ServiceTest {

    @Resource
    private Auto24Service auto24Service;

    @Test
//    @Disabled
    void search() {
        String search = auto24Service.search("876BCH");

        assertThat(search).contains("Sõiduki keskmine hind: 3200 € kuni 7200 €");
        assertThat(search).contains("Eestis registreerimise kuupäev (B1): 18.09.2009");
    }

    @Test
//    @Disabled
    void xsearch2() {
        String search = auto24Service.search("325BDV");

        assertThat(search).contains("Sõiduki keskmine hind: 1400 € kuni 1700 €");
        assertThat(search).contains("Eestis registreerimise kuupäev (B1): 12.05.2011");
    }
}
