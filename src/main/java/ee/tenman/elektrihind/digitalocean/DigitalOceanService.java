package ee.tenman.elektrihind.digitalocean;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class DigitalOceanService {

    private static final String DROPLET_ID = "384443548";

    @Resource
    private DigitalOceanClient digitalOceanClient;

    public void rebootDroplet() {
        digitalOceanClient.rebootDroplet(DROPLET_ID, Map.of("type", "reboot"));
    }

    public double getCpuUsagePercentage() {
        Instant now = Instant.now();
        Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);

        String start = String.valueOf(oneMinuteAgo.getEpochSecond());
        String end = String.valueOf(now.getEpochSecond());

        DigitalOceanResponse response = digitalOceanClient.getDropletCpuMetrics(DROPLET_ID, start, end);

        double totalCpuTime = 0.0001;
        double idleTime = 0;

        for (Result result : response.getData().getResult()) {
            for (List<String> value : result.getValues()) {
                double cpuTime = Double.parseDouble(value.get(1));
                totalCpuTime += cpuTime;
                if (result.getMetric().getMode().equals("idle")) {
                    idleTime += cpuTime;
                }
            }
        }

        return 100 * (totalCpuTime - idleTime) / totalCpuTime;
    }
}
