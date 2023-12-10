package ee.tenman.elektrihind.car.scrapeninja;

import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "scrapeninjaClient", url = "https://scrapeninja.p.rapidapi.com")
public interface ScrapeninjaClient {

    @PostMapping("/scrape")
    @Headers("Content-Type: application/json")
    ScrapeninjaResponse scrape(
            @RequestHeader("X-RapidAPI-Key") String apiKey,
            @RequestBody ScrapeninjaRequest request);
}
