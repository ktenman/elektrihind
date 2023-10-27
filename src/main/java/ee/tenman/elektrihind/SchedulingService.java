package ee.tenman.elektrihind;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ee.tenman.elektrihind.electricity.ElectricityPrice;
import ee.tenman.elektrihind.electricity.ElectricityPricesService;
import ee.tenman.elektrihind.telegram.TelegramService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class SchedulingService {

    @Resource
    private ElectricityPricesService electricityPricesService;

    @Resource
    private TelegramService telegramService;

    @Resource
    private Clock clock;

    private static final int DAILY_MESSAGE_LIMIT = 2;

    private static final LoadingCache<LocalDate, Integer> MESSAGE_COUNT_PER_DAY = CacheBuilder.newBuilder()
            .expireAfterWrite(7, TimeUnit.DAYS)
            .build(
                    new CacheLoader<>() {
                        @Override
                        public Integer load(LocalDate key) {
                            return 0;
                        }
                    }
            );

    private List<ElectricityPrice> latestPrices = new ArrayList<>();

    @Scheduled(cron = "0 59 * * * ?") // Runs every 60 minutes
    public void fetchAndSendPrices() {
        List<ElectricityPrice> electricityPrices = electricityPricesService.fetchDailyPrices();

        if (isNewPricesAvailable(electricityPrices)) {
            List<ElectricityPrice> filteredPrices = filterPricesForNext24Hours(electricityPrices);
            String formattedPrices = formatPricesForTelegram(filteredPrices);

            if (canSendMessageToday()) {
                telegramService.sendToTelegram(formattedPrices);
                incrementMessageCountForToday();
            }

            latestPrices = new ArrayList<>(electricityPrices);
        }
    }

    private boolean isNewPricesAvailable(List<ElectricityPrice> electricityPrices) {
        return !electricityPrices.equals(latestPrices);
    }

    private boolean canSendMessageToday() {
        LocalDate today = LocalDate.now(clock);
        return MESSAGE_COUNT_PER_DAY.getUnchecked(today) < DAILY_MESSAGE_LIMIT;
    }

    private void incrementMessageCountForToday() {
        LocalDate today = LocalDate.now(clock);
        int currentCount = MESSAGE_COUNT_PER_DAY.getUnchecked(today);
        MESSAGE_COUNT_PER_DAY.put(today, currentCount + 1);
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
        mostExpensiveHour.ifPresent(p -> builder.append("The most expensive: ").append(p));
        cheapestHour.ifPresent(p -> builder.append("The cheapest: ").append(p));

        return builder.toString();
    }
}
