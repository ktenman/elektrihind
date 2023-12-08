package ee.tenman.elektrihind.digitalocean;

import lombok.Data;

import java.util.List;

@Data
public class Result {
    private Metric metric;
    private List<List<String>> values; // This might need adjustment based on the actual structure
    // getters and setters
}
