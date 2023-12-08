package ee.tenman.elektrihind.digitalocean;

import lombok.Data;

import java.util.List;

@Data
public class DigitalOceanData {
    private String resultType;
    private List<Result> result;
    // getters and setters
}
