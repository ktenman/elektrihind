package ee.tenman.elektrihind.electricity;

import ee.tenman.elektrihind.config.HolidaysConfiguration;
import feign.FeignException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class ElectricityPricesService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Resource
    private ElectricityPricesClient electricityPricesClient;

    @Resource
    private HolidaysConfiguration holidaysConfiguration;


    @Retryable(value = FeignException.class, maxAttempts = 6, backoff = @Backoff(delay = 1500))
    public List<ElectricityPrice> fetchDailyPrices() {
        log.info("Fetching daily prices");
        List<ElectricityPrice> electricityPrices = electricityPricesClient.fetchDailyPrices();
        log.info("Fetched daily prices: {}", electricityPrices);
        return electricityPrices;
    }

    public boolean isHoliday(LocalDate date) {
        String formattedDate = date.format(FORMATTER);
        return holidaysConfiguration.getHolidays().contains(formattedDate);
    }

    public boolean isDayTime(LocalDateTime localDateTime) {
        int weekday = localDateTime.getDayOfWeek().getValue();
        int hour = localDateTime.getHour();

        boolean isWeekend = weekday == 6 || weekday == 7; // 6 for Saturday and 7 for Sunday
        return (hour >= 7 && hour < 22) && !isWeekend && !isHoliday(localDateTime.toLocalDate());
    }

    public boolean isNighttime(LocalDateTime localDateTime) {
        int weekday = localDateTime.getDayOfWeek().getValue();
        int hour = localDateTime.getHour();

        boolean isWeekend = weekday == 6 || weekday == 7; // 6 for Saturday and 7 for Sunday
        return (hour >= 22 || hour < 7) || isWeekend || isHoliday(localDateTime.toLocalDate());
    }


}
