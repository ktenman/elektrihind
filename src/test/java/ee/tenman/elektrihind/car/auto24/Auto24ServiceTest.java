package ee.tenman.elektrihind.car.auto24;

import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
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

    @Resource(name = "tenThreadExecutor")
    ExecutorService tenThreadExecutor;

    @Test
    @Disabled
    void solve() {

        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.runAsync(() -> auto24Service.solve("876BCH"), tenThreadExecutor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Test
    @Disabled
    void carPrice() {
        LinkedHashMap<String, String> result = auto24Service.carPrice("876BCH");

        System.out.println(result);
    }
}
