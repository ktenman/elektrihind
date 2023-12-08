package ee.tenman.elektrihind.digitalocean;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DigitalOceanService {

    private static final String DROPLET_ID = "384443548";

    @Resource
    private DigitalOceanClient digitalOceanClient;

    public void rebootDroplet() {
        digitalOceanClient.rebootDroplet(DROPLET_ID, Map.of("type", "reboot"));
    }
}
