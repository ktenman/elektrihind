package ee.tenman.elektrihind;

import ee.tenman.elektrihind.cache.CacheService;
import ee.tenman.elektrihind.electricity.ElectricityPrice;
import ee.tenman.elektrihind.electricity.ElectricityPricesService;
import ee.tenman.elektrihind.euribor.EuriborRateFetcher;
import ee.tenman.elektrihind.telegram.TelegramService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

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
    private EuriborRateFetcher euriborRateFetcher;

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
        String formattedPrices = telegramService.formatPricesForTelegram(filteredPrices);

        if (cacheService.canSendMessageToday()) {
            sendMessageAndIncrementCount(formattedPrices);
            log.info("Message sent successfully.");
        } else {
            log.info("Message sending limit reached for today.");
        }

        cacheService.setLatestPrices(electricityPrices);
    }

    @Scheduled(cron = "0 59 * * * ?") // Runs every 60 minutes
    public void fetchEuribor() {
        log.info("Fetching Euribor rates...");
        euriborRateFetcher.fetchEuriborRates();
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
}
