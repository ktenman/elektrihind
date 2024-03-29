package ee.tenman.elektrihind.digitalocean;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

import static ee.tenman.elektrihind.digitalocean.DigitalOceanClient.CLIENT_NAME;
import static ee.tenman.elektrihind.digitalocean.DigitalOceanClient.CLIENT_URL;

@FeignClient(name = CLIENT_NAME, url = CLIENT_URL, configuration = DigitalOceanClient.Configuration.class)
public interface DigitalOceanClient {

    String CLIENT_NAME = "digitalOceanClient";
    String CLIENT_URL = "https://api.digitalocean.com/v2";

    @PostMapping(value = "/droplets/{dropletId}/actions")
    void rebootDroplet(@PathVariable("dropletId") String dropletId, @RequestBody Map<String, String> action);

    class Configuration {
        @Value("${digitalocean.token}")
        private String token;

        @Bean
        public RequestInterceptor requestInterceptor() {
            return template -> template.header("Authorization", "Bearer " + token);
        }
    }
}
