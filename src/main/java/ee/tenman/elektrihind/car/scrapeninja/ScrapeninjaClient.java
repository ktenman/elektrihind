package ee.tenman.elektrihind.car.scrapeninja;

import feign.Headers;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "scrapeninjaClient", url = "https://scrapeninja.p.rapidapi.com", configuration = ScrapeninjaClient.Configuration.class)
public interface ScrapeninjaClient {

    @PostMapping("/scrape")
    @Headers("Content-Type: application/json")
    ScrapeninjaResponse scrape(@RequestBody ScrapeninjaRequest request);

    class Configuration {
        @Value("${scrapeninja.key}")
        private String apiKey;

        @Bean
        public RequestInterceptor requestInterceptor() {
            return template -> template.header("X-RapidAPI-Key", apiKey);
        }
    }
}
