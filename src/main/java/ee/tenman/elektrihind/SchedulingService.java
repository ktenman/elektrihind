package ee.tenman.elektrihind;

import ee.tenman.elektrihind.electricity.ElectricityPrice;
import ee.tenman.elektrihind.electricity.ElectricityPricesService;
import ee.tenman.elektrihind.telegram.TelegramService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class SchedulingService {

    @Resource
    private ElectricityPricesService electricityPricesService;

    @Resource
    private TelegramService telegramService;

    @Resource
    private Clock clock;

    @Scheduled(cron = "${scheduling.cronExpression}")
    public void fetchDailyPrices() {
        List<ElectricityPrice> electricityPrices = electricityPricesService.fetchDailyPrices();
        List<ElectricityPrice> filteredPrices = filterPricesForNext24Hours(electricityPrices);
        String formattedPrices = formatPricesForTelegram(filteredPrices);
        telegramService.sendToTelegram(formattedPrices);
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
