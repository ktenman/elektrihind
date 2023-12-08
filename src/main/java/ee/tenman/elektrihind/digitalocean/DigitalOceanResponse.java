package ee.tenman.elektrihind.digitalocean;

import lombok.Data;

@Data
public class DigitalOceanResponse {
    private String status;
    private DigitalOceanData data;
    // getters and setters
}
