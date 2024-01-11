package ee.tenman.elektrihind.movies;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

@Service
@Slf4j
public class DuckDuckGoClient {

    private static final String DIRECTED_BY_TEXT = "Directed by";
    private static final String SEARCH_URL_TEMPLATE = "https://duckduckgo.com/?q=%s+site:imdb.com";

    public Optional<String> searchForImdbLink(String title, String year, String director) {
        if (StringUtils.isBlank(title) || StringUtils.isBlank(director)) {
            log.warn("Invalid input provided. Title: '{}', Year: '{}', Director: '{}'", title, year, director);
            return Optional.empty();
        }

        log.info("Searching IMDb link for Title: {}, Year: {}, Director: {}", title, year, director);
        String query = URLEncoder.encode(String.join(" ", title, year, director), StandardCharsets.UTF_8);
        open(String.format(SEARCH_URL_TEMPLATE, query));

        ElementsCollection articles = $$(By.tagName("article"))
                .filter(Condition.text(DIRECTED_BY_TEXT))
                .filter(Condition.text(director));

        if (year != null) {
            articles = articles.filter(Condition.text(year));
        }

        SelenideElement movieResult = articles.first();

        if (!movieResult.exists()) {
            log.warn("IMDb link not found for Title: {}, Year: {}, Director: {}", title, year, director);
            return Optional.empty();
        }

        movieResult.click();
        String currentUrl = WebDriverRunner.url();

        if (!currentUrl.contains("title")) {
            log.warn("Invalid IMDb URL structure after click: {}", currentUrl);
            return Optional.empty();
        }

        log.info("Found IMDb link: {} for Title: {}, Year: {}, Director: {}", currentUrl, title, year, director);
        return Optional.of(currentUrl);
    }

    public Optional<String> searchForImdbLink(String title, String director) {
        return searchForImdbLink(title, null, director);
    }
}
