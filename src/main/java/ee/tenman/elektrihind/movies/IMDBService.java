package ee.tenman.elektrihind.movies;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class IMDBService {

    private static final Pattern IMDB_ID_PATTERN = Pattern.compile("/title/(tt\\d+)/");

    private static String extractIMDbIdFromHref(String href) {
        Matcher matcher = IMDB_ID_PATTERN.matcher(href);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    @Retryable(maxAttempts = 7, backoff = @Backoff(delay = 7777))
    public Optional<String> getImdbRatingV2(String imdbId) {
        try {
            log.info("Fetching IMDB rating for ID: {}", imdbId);
            Document document = Jsoup.connect("https://www.imdb.com/title/" + imdbId).get();
            Element element = document.selectFirst("[data-testid=\"hero-rating-bar__aggregate-rating__score\"] span");
            return Optional.ofNullable(element).map(Element::text);
        } catch (Exception e) {
            log.error("Error fetching IMDB rating for ID: {}", imdbId, e);
        } finally {
            log.info("Fetched IMDB rating for ID: {}", imdbId);
        }
        return Optional.empty();
    }

    @Retryable(maxAttempts = 7, backoff = @Backoff(delay = 7777))
    public Optional<String> getIMDbId(String text) {
        try {
            log.info("Fetching movie imdb ID for text: {}", text);
            String encodedQuery = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String imdbURL = "https://www.imdb.com/find/?q=" + encodedQuery;
            Document document = Jsoup.connect(imdbURL).get();
            Elements listItems = document.select("li.ipc-metadata-list-summary-item");
            for (Element listItem : listItems) {
                Element anchor = listItem.selectFirst("a");
                if (anchor != null) {
                    String href = anchor.attr("href");
                    String imdbId = extractIMDbIdFromHref(href);
                    return Optional.ofNullable(imdbId);
                }
            }

        } catch (Exception e) {
            log.error("Error fetching movie imdb ID for text: {}", text, e);
        }
        return Optional.empty();
    }
}
