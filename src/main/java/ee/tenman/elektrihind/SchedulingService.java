package ee.tenman.elektrihind;

import ee.tenman.elektrihind.electricity.ElectricityPrice;
import ee.tenman.elektrihind.electricity.ElectricityPricesService;
import ee.tenman.elektrihind.telegram.TelegramService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class SchedulingService {

    @Resource
    private ElectricityPricesService electricityPricesService;

    @Resource
    private TelegramService telegramService;

    @Resource
    private CacheService cacheService;

    @Resource
    private Clock clock;

    @Scheduled(cron = "0 59 * * * ?") // Runs every 60 minutes
    public void fetchAndSendPrices() {
        log.info("Fetching and sending prices...");

        List<ElectricityPrice> electricityPrices = electricityPricesService.fetchDailyPrices();
        log.debug("Fetched {} prices.", electricityPrices.size());

        if (!isNewPricesAvailable(electricityPrices)) {
            log.info("No new prices available. Skipping send.");
            return;
        }

        List<ElectricityPrice> filteredPrices = filterPricesForNext24Hours(electricityPrices);
        String formattedPrices = formatPricesForTelegram(filteredPrices);

        if (cacheService.canSendMessageToday()) {
            sendMessageAndIncrementCount(formattedPrices);
            log.info("Message sent successfully.");
        } else {
            log.info("Message sending limit reached for today.");
        }

        cacheService.setLatestPrices(electricityPrices);
    }

    void sendMessageAndIncrementCount(String formattedPrices) {
        log.debug("Sending message: {}", formattedPrices);
        telegramService.sendToTelegram(formattedPrices);
        cacheService.incrementMessageCountForToday();
        log.debug("Message count for today incremented.");
    }

    boolean isNewPricesAvailable(List<ElectricityPrice> electricityPrices) {
        return !electricityPrices.equals(cacheService.getLatestPrices());
    }

    public List<ElectricityPrice> filterPricesForNext24Hours(List<ElectricityPrice> electricityPrices) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime twentyFourHoursLater = now.plusHours(24);
        return electricityPrices.stream()
                .filter(price -> !price.getDate().isBefore(now) && price.getDate().isBefore(twentyFourHoursLater))
                .toList();
    }

    public String formatPricesForTelegram(List<ElectricityPrice> filteredPrices) {
        StringBuilder builder = new StringBuilder();
        filteredPrices.forEach(builder::append);

        Optional<ElectricityPrice> mostExpensiveHour = filteredPrices.stream().max(Comparator.comparingDouble(ElectricityPrice::getPrice));
        Optional<ElectricityPrice> cheapestHour = filteredPrices.stream().min(Comparator.comparingDouble(ElectricityPrice::getPrice));

        builder.append("\n");
        mostExpensiveHour.ifPresent(p -> {
            builder.append(priceDateLabel(p.getDate())).append("the most expensive: ").append(p);
            log.debug("Most expensive price: {}", p);
        });
        cheapestHour.ifPresent(p -> {
            builder.append(priceDateLabel(p.getDate())).append("the cheapest: ").append(p);
            log.debug("Cheapest price: {}", p);
        });

        return builder.toString();
    }

    String priceDateLabel(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (dateTime.toLocalDate().isEqual(now.toLocalDate())) {
            return "Today, ";
        } else if (dateTime.toLocalDate().isEqual(now.plusDays(1).toLocalDate())) {
            return "Tomorrow, ";
        } else {
            return "";
        }
    }
}
