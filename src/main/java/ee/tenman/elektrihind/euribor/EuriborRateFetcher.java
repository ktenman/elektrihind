package ee.tenman.elektrihind.euribor;

import ee.tenman.elektrihind.cache.CacheService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static ee.tenman.elektrihind.config.RedisConfig.TEN_MINUTES;


@Service
@Slf4j
public class EuriborRateFetcher {

    private static final String DATE_PATTERN = "M/d/yyyy";
    private static final String EURIBOR_RATES_URL = "https://www.euribor-rates.eu/en/current-euribor-rates/3/euribor-rate-6-months/";
    private final TreeMap<LocalDate, BigDecimal> rates = new TreeMap<>(Collections.reverseOrder());

    @Resource
    private CacheService cacheService;

    public SortedMap<LocalDate, BigDecimal> getRates() {
        if (rates.isEmpty()) {
            fetchEuriborRates();
        }
        return rates;
    }

    @Cacheable(TEN_MINUTES)
    public String getEuriborRateResponse() {
        if (getRates().size() < 2) {
            return "Not enough data to calculate Euribor rate change.";
        }

        Iterator<Map.Entry<LocalDate, BigDecimal>> iterator = getRates().entrySet().iterator();

        Map.Entry<LocalDate, BigDecimal> latestEntry = iterator.next();
        Map.Entry<LocalDate, BigDecimal> previousEntry = iterator.hasNext() ? iterator.next() : null;

        if (latestEntry == null || previousEntry == null) {
            return "Unable to fetch the latest Euribor rates.";
        }

        LocalDate latestDate = latestEntry.getKey();
        BigDecimal latestRate = latestEntry.getValue();
        BigDecimal previousRate = previousEntry.getValue();

        BigDecimal rateChange = latestRate.subtract(previousRate);
        String changeSymbol = "";
        String emoji = "";

        if (rateChange.compareTo(BigDecimal.ZERO) > 0) {
            changeSymbol = "+";
            emoji = "\ud83d\udc4e"; // 👎 for increase
        } else if (rateChange.compareTo(BigDecimal.ZERO) < 0) {
            changeSymbol = "-";
            emoji = "\ud83d\udc4d"; // 👍 for decrease
        } else {
            emoji = "\u270A"; // ✊ for no change
        }

        return String.format("Euribor for %s is %.3f (%s%.3f) %s",
                latestDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                latestRate,
                changeSymbol,
                rateChange.abs(), // use the absolute value for display
                emoji);
    }

    public void fetchEuriborRates() {
        try {
            Document doc = Jsoup.connect(EURIBOR_RATES_URL).get();
            Element byDayDiv = doc.select(".card").first();
            if (byDayDiv == null) {
                log.error("No 'By day' section found.");
            }
            parseRates(byDayDiv, rates);
        } catch (Exception e) {
            log.error("Error fetching data: {}", e.getMessage(), e);
        }
    }

    private void parseRates(Element byDayDiv, TreeMap<LocalDate, BigDecimal> rates) {
        Element table = byDayDiv.select("table").first();
        if (table == null) {
            log.error("No table found in the 'By day' section.");
            return;
        }
        Elements rows = table.select("tbody tr");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        for (Element row : rows) {
            try {
                LocalDate date = parseDate(row.select("td").get(0).text(), formatter);
                BigDecimal rate = parseRate(row.select("td").get(1).text());
                rates.put(date, rate);
            } catch (DateTimeParseException e) {
                log.error("Error parsing row: {} - {}", row, e.getMessage());
            }
        }
    }

    private LocalDate parseDate(String dateText, DateTimeFormatter formatter) throws DateTimeParseException {
        return LocalDate.parse(dateText, formatter);
    }

    private BigDecimal parseRate(String rateText) {
        return new BigDecimal(rateText.replace("%", "").trim());
    }

    public BigDecimal fetchLatestEuriborRateAndUpdateCache() {
        fetchEuriborRates();
        BigDecimal latestRate = getLatestEuriborRate();
        if (latestRate != null) {
            cacheService.setLastEuriborRate(latestRate);
        }
        return latestRate;
    }

    public BigDecimal getLatestEuriborRate() {
        return rates.isEmpty() ? null : rates.firstEntry().getValue();
    }
}
