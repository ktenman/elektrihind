package ee.tenman.elektrihind.electricity;

import ee.tenman.elektrihind.cache.CacheService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ElectricityPriceResponder {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final CacheService cacheService;
    private final Clock clock;

    public ElectricityPriceResponder(CacheService cacheService, Clock clock) {
        this.cacheService = cacheService;
        this.clock = clock;
    }

    public String getElectricityPriceResponse() {
        List<ElectricityPrice> electricityPrices = cacheService.getLatestPrices();
        Optional<ElectricityPrice> currentPrice = currentPrice(electricityPrices);
        if (currentPrice.isEmpty()) {
            return "Could not find current electricity price.";
        }

        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("Current electricity price is ")
                .append(currentPrice.map(ElectricityPrice::getPrice).orElseThrow())
                .append(" cents/kWh.\n");

        LocalDateTime now = LocalDateTime.now(clock);
        List<ElectricityPrice> upcomingPrices = electricityPrices.stream()
                .filter(price -> price.getDate().isAfter(now))
                .sorted(Comparator.comparing(ElectricityPrice::getDate))
                .toList();

        if (upcomingPrices.isEmpty()) {
            responseBuilder.append("No upcoming price data available.");
        } else {
            responseBuilder.append("Upcoming prices:\n");
            for (ElectricityPrice price : upcomingPrices) {
                responseBuilder.append(price.getDate().format(DATE_TIME_FORMATTER))
                        .append(" - ")
                        .append(price.getPrice())
                        .append("\n");
            }
        }
        return responseBuilder.toString();
    }

    private Optional<ElectricityPrice> currentPrice(List<ElectricityPrice> electricityPrices) {
        LocalDateTime now = LocalDateTime.now(clock);
        return electricityPrices.stream()
                .filter(price -> price.getDate().isBefore(now.plusMinutes(1)) && price.getDate().isAfter(now.minusMinutes(59)))
                .findFirst();
    }
}
