package ee.tenman.elektrihind.digitalocean;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class DigitalOceanService {
    private static final String DROPLET_ID = "384443548";
    @Resource
    private DigitalOceanClient digitalOceanClient;

    public double getCpuUsagePercentage() {
        DigitalOceanResponse response = getDropletCpuMetricsForPastMinute();
        return calculateCpuUsagePercentage(response);
    }

    private DigitalOceanResponse getDropletCpuMetricsForPastMinute() {
        return digitalOceanClient.getDropletCpuMetrics(DROPLET_ID,
                String.valueOf(Instant.now().minus(1, ChronoUnit.MINUTES).getEpochSecond()),
                String.valueOf(Instant.now().getEpochSecond()));
    }

    private double calculateCpuUsagePercentage(DigitalOceanResponse response) {
        double totalCpuTime = 0.0001;
        double idleTime = 0;
        for (Result result : response.getData().getResult()) {
            Pair<Double, Double> cpuTimes = calculateCpuTimesFor(result);
            totalCpuTime += cpuTimes.getKey();
            idleTime += cpuTimes.getValue();
        }
        return calculatePercentage(totalCpuTime, idleTime);
    }

    private Pair<Double, Double> calculateCpuTimesFor(Result result) {
        double totalCpuTime = 0;
        double idleTime = 0;
        for (List<String> value : result.getValues()) {
            double cpuTime = Double.parseDouble(value.get(1));
            totalCpuTime += cpuTime;
            if (result.getMetric().getMode().equals("idle")) {
                idleTime += cpuTime;
            }
        }
        return Pair.of(totalCpuTime, idleTime);
    }

    private double calculatePercentage(double totalCpuTime, double idleTime) {
        return 100 * (totalCpuTime - idleTime) / totalCpuTime;
    }

    public void rebootDroplet() {
        return;
    }
}
