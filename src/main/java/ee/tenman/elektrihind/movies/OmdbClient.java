package ee.tenman.elektrihind.movies;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "omdbClient", url = "http://www.omdbapi.com")
public interface OmdbClient {

    @GetMapping
    MovieDetails fetchMovieDetails(
            @RequestParam("t") String title,
            @RequestParam("y") String year,
            @RequestParam("apikey") String apiKey
    );
}
