package ee.tenman.elektrihind.movies.imdb;

import feign.FeignException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class IMDBService {

    @Resource
    private IMDBApiClient imdbApiClient;

    @Retryable(value = FeignException.class, maxAttempts = 7, backoff = @Backoff(delay = 7777))
    public Optional<Double> getImdbRating(String imdbId) {
        log.info("Fetching movie details for ID: {}", imdbId);
        IMDB movieDetails = imdbApiClient.fetchMovie(imdbId);
        log.info("Fetched movie details: {}", movieDetails);
        return Optional.ofNullable(movieDetails)
                .map(IMDB::getRating)
                .map(IMDB.Rating::getStar);
    }
}
