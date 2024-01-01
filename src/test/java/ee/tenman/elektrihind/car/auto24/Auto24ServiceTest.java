package ee.tenman.elektrihind.car.auto24;

import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@IntegrationTest
class Auto24ServiceTest {

    @Resource
    Auto24Service auto24Service;

    @Resource(name = "xThreadExecutor")
    ExecutorService xThreadExecutor;

    @Test
    @Disabled
    void solve() {

        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            futures.add(CompletableFuture.runAsync(() -> auto24Service.solve("876BCH"), xThreadExecutor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Test
    @Disabled
    void solve2() {
        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            futures.add(CompletableFuture.runAsync(() -> auto24Service.solve("876BCH"), xThreadExecutor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Test
    @Disabled
    void carPrice() {
        LinkedHashMap<String, String> result = auto24Service.carPrice("876BCH");

        System.out.println(result);
        Assertions.assertThat(result).isNotEmpty()
                .containsKey("Turuhind")
                .containsValue("3100 € kuni 7200 €\n");
    }
}
