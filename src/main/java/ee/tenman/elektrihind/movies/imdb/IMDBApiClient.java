package ee.tenman.elektrihind.movies.imdb;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "iMDBApiClient", url = "${imdb-api.url}")
public interface IMDBApiClient {
    @GetMapping("/title/{imdbId}")
    IMDB fetchMovie(@PathVariable String imdbId);
}
