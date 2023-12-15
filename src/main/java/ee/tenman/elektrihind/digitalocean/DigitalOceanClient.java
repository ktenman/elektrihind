package ee.tenman.elektrihind.digitalocean;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "digitalOceanClient", url = "https://api.digitalocean.com/v2", configuration = DigitalOceanClient.Configuration.class)
public interface DigitalOceanClient {

    @PostMapping(value = "/droplets/{dropletId}/actions")
    void rebootDroplet(@PathVariable("dropletId") String dropletId, @RequestBody Map<String, String> action);

    @GetMapping(value = "/monitoring/metrics/droplet/cpu")
    DigitalOceanResponse getDropletCpuMetrics(@RequestParam("host_id") String hostId,
                                              @RequestParam("start") String start,
                                              @RequestParam("end") String end);

    class Configuration {
        @Value("${digitalocean.token}")
        private String token;

        @Bean
        public RequestInterceptor requestInterceptor() {
            return template -> template.header("Authorization", "Bearer " + token);
        }
    }
}
