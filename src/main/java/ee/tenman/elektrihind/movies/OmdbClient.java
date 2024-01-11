package ee.tenman.elektrihind.movies;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static ee.tenman.elektrihind.movies.OmdbClient.CLIENT_NAME;
import static ee.tenman.elektrihind.movies.OmdbClient.CLIENT_URL;

@FeignClient(name = CLIENT_NAME, url = CLIENT_URL, configuration = OmdbClient.Configuration.class)
public interface OmdbClient {

    String CLIENT_NAME = "omdbClient";
    String CLIENT_URL = "http://www.omdbapi.com";

    @GetMapping
    MovieDetails fetchMovieDetails(@RequestParam("t") String title, @RequestParam("y") String year);

    @GetMapping
    MovieDetails fetchMovieDetails(@RequestParam("t") String title);

    class Configuration {
        @Value("${omdb-api.key}")
        private String omdbApiKey;

        @Bean
        public RequestInterceptor requestInterceptor() {
            return (RequestTemplate requestTemplate) -> requestTemplate.query("apikey", omdbApiKey);
        }
    }
}
