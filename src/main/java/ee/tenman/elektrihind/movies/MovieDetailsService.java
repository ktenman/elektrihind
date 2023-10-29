package ee.tenman.elektrihind.movies;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MovieDetailsService {

    @Resource
    private OmdbClient omdbClient;

    @Value("${omdb-api.key}")
    private String omdbApiKey;

    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1500))
    public MovieDetails fetchMovieDetails(String title, String year) {
        log.info("Fetching details for movie: {}", title);
        MovieDetails details = omdbClient.fetchMovieDetails(title, year, omdbApiKey);
        log.info("Fetched details: {}", details);
        return details;
    }
}
