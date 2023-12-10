package ee.tenman.elektrihind.car.scrapeninja;

import lombok.Data;

@Data
public class ScrapeninjaRequest {
    private String url;
    private String[] headers;
    private String method;
    private String data;
}
