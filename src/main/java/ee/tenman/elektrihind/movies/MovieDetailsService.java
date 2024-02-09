package ee.tenman.elektrihind.movies;

import ee.tenman.elektrihind.cache.CacheService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Stream;

@Service
@Slf4j
public class MovieDetailsService {

    @Resource
    private OmdbClient omdbClient;

    @Resource
    private CacheService cacheService;

    @Resource
    private IMDBService imdbService;

    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1500))
    public MovieDetails fetchMovieDetails(String title, String year) {
        log.info("Fetching details for movie: {}", title);
        MovieDetails details = omdbClient.fetchMovieDetails(title, year);
        log.info("Fetched details: {}", details);
        return details;
    }

    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1500))
    public Optional<MovieDetails> fetchMovieDetails(String title) {
        try {
            Optional<MovieDetails> cachedDetails = cacheService.fetchMovieDetails(title);
            if (cachedDetails.isPresent()) {
                return cachedDetails;
            }

            log.info("Fetching details for movie: {}", title);
            Optional<MovieDetails> movieDetails = Stream.of(null, "2024", "2023")
                    .map(year -> fetchDetailsAndUpdateRating(title, year))
                    .filter(details -> details.getImdbRating() != null && !"N/A".equals(details.getImdbRating()))
                    .findFirst()
                    .or(() -> {
                        Optional<String> imdbId = imdbService.getIMDbId(title);
                        if (imdbId.isPresent()) {
                            Optional<String> imdbRating = imdbService.getImdbRatingV2(imdbId.get());
                            Optional<MovieDetails> optionalMovieDetails = imdbRating.map(rating -> MovieDetails.builder()
                                    .imdbID(imdbId.get())
                                    .imdbRating(rating)
                                    .build());
                            optionalMovieDetails.ifPresent(details -> cacheService.saveMovieDetails(title, details));
                            return optionalMovieDetails;
                        }
                        return Optional.empty();
                    });

            if (movieDetails.isPresent()) {
                cacheService.saveMovieDetails(title, movieDetails.get());
                return movieDetails;
            }

            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to fetch details for movie: {}", title, e);
            return Optional.empty();
        }
    }

    private MovieDetails fetchDetailsAndUpdateRating(String title, String year) {
        MovieDetails details;
        if (year == null) {
            details = omdbClient.fetchMovieDetails(title);
        } else {
            details = omdbClient.fetchMovieDetails(title, year);
        }
        updateImdbRating(details);
        return details;
    }

    private void updateImdbRating(MovieDetails details) {
        if (details.getImdbID() != null &&
                (details.getImdbRating() == null || "N/A".equals(details.getImdbRating()))) {
            imdbService.getImdbRatingV2(details.getImdbID()).ifPresent(details::setImdbRating);
        }
    }

}
