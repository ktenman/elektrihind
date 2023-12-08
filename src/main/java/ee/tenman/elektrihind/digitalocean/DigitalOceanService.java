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

    public static String[] getLast5Seconds() {
        Instant now = Instant.now();
        Instant oneMinuteAgo = now.minus(5, ChronoUnit.SECONDS);

        String start = String.valueOf(oneMinuteAgo.getEpochSecond());
        String end = String.valueOf(now.getEpochSecond());

        return new String[]{start, end};
    }

    public double getCpuUsagePercentage() {
        String[] last5Seconds = getLast5Seconds();
        DigitalOceanResponse response = digitalOceanClient.getDropletCpuMetrics(DROPLET_ID, last5Seconds[0], last5Seconds[1]);

        double totalCpuTime = 0.000000000000001;
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
