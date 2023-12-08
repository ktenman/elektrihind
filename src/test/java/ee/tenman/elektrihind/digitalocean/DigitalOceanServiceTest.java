package ee.tenman.elektrihind.digitalocean;

import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@IntegrationTest
class DigitalOceanServiceTest {

    @Resource
    private DigitalOceanService digitalOceanService;

    @Test
    @Disabled
    void getCpuMetrics() {
        double cpuMetrics = digitalOceanService.getCpuUsagePercentage();
        System.out.println(cpuMetrics);
    }
}
