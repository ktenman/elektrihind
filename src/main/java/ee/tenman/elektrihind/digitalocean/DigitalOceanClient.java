package ee.tenman.elektrihind.digitalocean;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "digitalOceanClient", url = "https://api.digitalocean.com/v2", configuration = FeignClientConfig.class)
public interface DigitalOceanClient {

    @PostMapping("/droplets/{dropletId}/actions")
    void rebootDroplet(@PathVariable("dropletId") String dropletId, @RequestBody Map<String, String> action);
}
