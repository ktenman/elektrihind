package ee.tenman.elektrihind.movies;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import ee.tenman.elektrihind.movies.imdb.IMDBService;
import ee.tenman.elektrihind.telegram.TelegramService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

@Slf4j
@Service
public class MovieScraper {

    private static final String FILE_NAME = "movies.csv";
    private static final int UPPER_LIMIT = 999;

    @Resource
    private MovieDetailsService movieDetailsService;

    @Resource
    private TelegramService telegramService;

    @Resource
    private DuckDuckGoClient duckDuckGoClient;

    @Resource
    private IMDBService imdbService;

    @PostConstruct
    public void fetchMovies() {
        Instant startTime = Instant.now();
        log.info("Starting movie fetching process.");
        try {
            initializeBrowser();
            List<String> movieUrls = fetchMovieUrls();
            List<MovieDto> movies = fetchMovieDetailsFromUrls(movieUrls);

            log.info("In total found movies: {}", movies.size());
            movies.sort(Comparator.comparing(MovieDto::getImdbRating).reversed());

            writeMoviesToCsv(movies);
            telegramService.sendCsvToTelegram(FILE_NAME);
        } catch (Exception e) {
            log.error("Error fetching movies", e);
        } finally {
            Instant endTime = Instant.now();
            log.info("Movie fetching process completed in: {} seconds", Duration.between(startTime, endTime).getSeconds());
        }
    }

    private void initializeBrowser() {
        log.info("Initializing browser configurations.");
        Configuration.browser = "firefox";
        Configuration.headless = true;
        open("https://poff.ee/otsi_filmi/index.html");
    }

    private List<String> fetchMovieUrls() {
        log.info("Fetching movie URLs.");
        List<String> urls = new ArrayList<>();
        ElementsCollection elementsCollection = $$(By.tagName("a"))
                .filter(Condition.attributeMatching("href", ".*/film/.*"));
        for (SelenideElement element : elementsCollection) {
            urls.add(element.attr("href"));
        }
        return urls;
    }

    private List<MovieDto> fetchMovieDetailsFromUrls(List<String> movieUrls) {
        List<MovieDto> movies = new ArrayList<>();
        log.info("Fetching details for each movie URL.");

        for (int i = 0; i < Math.min(UPPER_LIMIT, movieUrls.size()); i++) {
            String url = movieUrls.get(i);
            movies.add(fetchMovieDetailsFromUrl(url));
        }

        return movies;
    }

    private MovieDto fetchMovieDetailsFromUrl(String url) {
        log.info("Fetching details for movie URL: {}", url);
        open(url);
        waitForElementToBeVisible(By.className("str_info_value"));
        return fetchMovieDetailsFromCurrentPage(url);
    }

    private void waitForElementToBeVisible(By selector) {
        Selenide.$(selector).shouldBe(Condition.visible, Duration.ofSeconds(10));
    }

    private MovieDto fetchMovieDetailsFromCurrentPage(String url) {
        log.info("Fetching details from URL: {}", url);

        MovieDto.MovieDtoBuilder movieDtoBuilder = MovieDto.builder()
                .title(fetchElementText(By.className("str_info_value")))
                .year(fetchInfoGridText(".*Aasta.*"))
                .director(fetchInfoGridText(".*Režissöör.*"))
                .country(fetchInfoGridText(".*Riik.*"))
                .duration(fetchInfoLabelSiblingText(".*Kestus.*"))
                .language(fetchInfoLabelSiblingText(".*Keel.*"))
                .imdbRating(-1.0)
                .imdbLink("")
                .poffUrl(url);

        MovieDetails movieDetails = movieDetailsService.fetchMovieDetails(
                movieDtoBuilder.build().getTitle(),
                movieDtoBuilder.build().getYear());

        if (movieDetails == null || movieDetails.getImdbID() == null) {
            return movieDtoBuilder.build();
        }

        movieDtoBuilder.imdbLink(movieDetails.getImdbLink());
        try {
            movieDtoBuilder.imdbRating(Double.parseDouble(movieDetails.getImdbRating()));
        } catch (NumberFormatException e) {
            log.warn("Invalid IMDb rating format for movie: {}", movieDtoBuilder.build().getTitle());
        }

        MovieDto movieDto = movieDtoBuilder.build();

        if (movieDto.getImdbRating() == -1.0) {
            imdbService.getImdbRating(movieDto.getImdbId()).ifPresentOrElse(
                    newRating -> {
                        movieDto.setImdbRating(newRating);
                        log.info("Updated rating for movie {}: {}", movieDto.getTitle(), newRating);
                    },
                    () -> log.warn("Failed to fetch IMDb rating for movie: {}", movieDto.getTitle())
            );
        }

        updateMovieData(movieDto);

        return movieDto;
    }

    private String fetchElementText(By selector) {
        return Selenide.$(selector).text();
    }

    private String fetchInfoGridText(String pattern) {
        ElementsCollection elements = $$(By.className("str_info_grid_3"))
                .filter(Condition.matchText(pattern));
        return elements.isEmpty() ? "" : elements.first().lastChild().text();
    }

    private String fetchInfoLabelSiblingText(String pattern) {
        ElementsCollection elements = $$(By.className("str_info_label"))
                .filter(Condition.matchText(pattern));
        return elements.isEmpty() ? "" : elements.first().sibling(0).text();
    }

    private void writeMoviesToCsv(List<MovieDto> movies) {
        log.info("Writing movies to CSV file: {}", FILE_NAME);

        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(Paths.get(FILE_NAME)), StandardCharsets.UTF_8)) {
            writeCsvHeader(writer);
            movies.forEach(movie -> writeMovieToCsv(writer, movie));
        } catch (IOException e) {
            log.error("Error writing CSV file", e);
        }
    }

    private void writeCsvHeader(OutputStreamWriter writer) throws IOException {
        writer.write("Title;Year;Director;Country;Duration;Language;PoffUrl;IMDbRating;IMDbId\n");
    }

    private void writeMovieToCsv(OutputStreamWriter writer, MovieDto movie) {
        try {
            writer.write(formatMovieAsCsvString(movie));
        } catch (IOException e) {
            log.error("Error writing movie {} to CSV", movie.getTitle(), e);
        }
    }

    private String formatMovieAsCsvString(MovieDto movie) {
        char separator = ';';
        return movie.getTitle() +
                separator + movie.getYear() +
                separator + movie.getDirector() +
                separator + movie.getCountry() +
                separator + movie.getDuration() +
                separator + movie.getLanguage() +
                separator + movie.getPoffUrl() +
                separator + movie.getImdbRating() +
                separator + movie.getImdbLink() + '\n';
    }

    public void updateMovieData(MovieDto movie) {
        boolean hasUpdates = false;

        if (needsLinkUpdate(movie)) {
            updateMovieLink(movie);
            hasUpdates = true;
        }

        if (needsRatingUpdate(movie)) {
            updateMovieRating(movie);
            hasUpdates = true;
        }

        if (hasUpdates) {
            log.info("Updated movie: {}", movie.getTitle());
        }
    }

    private boolean needsRatingUpdate(MovieDto movie) {
        return movie.getImdbRating() == -1.0 && isValidLink(movie.getImdbLink());
    }

    private boolean needsLinkUpdate(MovieDto movie) {
        return !isValidLink(movie.getImdbLink());
    }

    private boolean isValidLink(String link) {
        return Objects.nonNull(link) && !link.isEmpty();
    }

    private void updateMovieLink(MovieDto movie) {
        duckDuckGoClient.searchForImdbLink(movie.getTitle(), movie.getYear(), movie.getDirector())
                .ifPresent(link -> {
                    movie.setImdbLink(link);
                    log.info("Updated IMDb link for movie {}: {}", movie.getTitle(), link);
                });
    }

    private void updateMovieRating(MovieDto movie) {
        imdbService.getImdbRating(movie.getImdbId()).ifPresentOrElse(
                newRating -> {
                    movie.setImdbRating(newRating);
                    log.info("Updated rating for movie {}: {}", movie.getTitle(), newRating);
                },
                () -> log.warn("Failed to fetch IMDb rating for movie: {}", movie.getTitle())
        );
    }
}
