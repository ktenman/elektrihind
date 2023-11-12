package ee.tenman.elektrihind.electricity;

import ee.tenman.elektrihind.config.HolidaysConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ElekterBotService extends TelegramLongPollingBot {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Resource
    private HolidaysConfiguration holidaysConfiguration;

    @Resource
    private Clock clock;

    @Resource
    private ElectricityPricesService electricityPricesService;

    @Value("${telegram.elektriteemu.token}")
    private String token;

    @Value("${telegram.elektriteemu.username}")
    private String username;

    List<String[]> readCsv(String filePath) {
        List<String[]> data = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            for (String line : lines) {
                if (line.startsWith("Algus")) { // Skip until the headers of the actual data
                    data.add(line.split(";"));
                } else if (line.contains(";")) { // Read the actual data lines
                    data.add(line.split(";"));
                }
            }
        } catch (IOException e) {
            log.error("Failed to read CSV file: {}", filePath, e);
        }
        return data;
    }

    @PostConstruct
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
        } catch (TelegramApiException e) {
            log.error("Error occurred while initializing the bot.", e);
        }
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            return;
        }

        Message message = update.getMessage();
        long chatId = message.getChatId();

        if (message.hasText()) {
            handleTextMessage(message, chatId);
        } else if (message.hasDocument()) {
            handleDocumentMessage(message, chatId);
        }
    }

    private void handleTextMessage(Message message, long chatId) {
        String messageText = message.getText();
        Pattern durationPattern = Pattern.compile("parim hind (\\d+) min", Pattern.CASE_INSENSITIVE);
        Matcher matcher = durationPattern.matcher(messageText);

        if ("/start".equals(messageText)) {
            sendMessage(chatId, "Hello! I am an electricity bill calculator bot. Please send me a CSV file.");
        } else if (messageText.toLowerCase().contains("elektrihind")) {
            handleElectricityPrice(chatId);
        } else if (matcher.find()) {
            handleDurationMessage(matcher, chatId);
        } // Consider adding an else block for unhandled text messages
    }

    private void handleDocumentMessage(Message message, long chatId) {
        Document document = message.getDocument();
        String fileName = document.getFileName();
        if (fileName != null && fileName.endsWith(".csv")) {
            handleCsvDocument(document, chatId);
        } else {
            sendMessage(chatId, "Please send a CSV file.");
        }
    }

    private void handleElectricityPrice(long chatId) {
        List<ElectricityPrice> electricityPrices = electricityPricesService.fetchDailyPrices();
        Double currentPrice = currentPrice(electricityPrices); // Assume currentPrice is a method to calculate current price
        String response = "Current electricity price is " + currentPrice + " cents/kWh.";
        sendMessage(chatId, response);
    }


    private void handleDurationMessage(Matcher matcher, long chatId) {
        int durationInMinutes = Integer.parseInt(matcher.group(1));
        List<ElectricityPrice> electricityPrices = electricityPricesService.fetchDailyPrices()
                .stream()
                .filter(electricityPrice -> electricityPrice.getDate().isAfter(LocalDateTime.now(clock)))
                .toList();
        BestPriceResult bestPrice = PriceFinder.findBestPriceForDuration(electricityPrices, durationInMinutes);

        if (bestPrice != null) {
            String response = formatBestPriceResponse(bestPrice);
            sendMessage(chatId, response);
        } else {
            sendMessage(chatId, "Could not calculate the best time to start your washing machine.");
        }
    }

    String formatBestPriceResponse(BestPriceResult bestPrice) {
        return "Best time to start is " + bestPrice.getStartTime() +
                " with average price of " + bestPrice.getAveragePrice() + " cents/kWh. " +
                "Total cost is " + bestPrice.getTotalCost() + " EUR. In " +
                Duration.between(LocalDateTime.now(clock), bestPrice.getStartTime()).toHours() + " hours.";
    }

    Double currentPrice(List<ElectricityPrice> electricityPrices) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime key = LocalDateTime.of(now.toLocalDate(),
                now.toLocalTime().withHour(now.getHour()).withMinute(0).withSecond(0).withNano(0));
        return electricityPrices.stream().filter(d -> d.getDate().equals(key))
                .findFirst().map(ElectricityPrice::getPrice)
                .orElse(null);
    }

    void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message: {}", text, e);
        }
    }

    void handleCsvDocument(Document document, long chatId) {
        String filePath = null;
        try {
            String fileId = document.getFileId();
            // Get the file path from Telegram servers
            java.io.File file = downloadTelegramFile(fileId);
            filePath = file.getAbsolutePath();

            // Process the CSV file (You'll need to implement readCsv and calculateTotalCost)
            List<String[]> data = readCsv(filePath);
            CostCalculationResult result = calculateTotalCost(data.subList(7, data.size())); // Assuming first 7 rows are headers

            // Construct the response message
            String response = String.format(
                    "Total electricity consumed: %.3f kWh\n" +
                            "Total cost: %.2f EUR\n" +
                            "Daytime electricity consumed: %.3f kWh\n" +
                            "Nighttime electricity consumed: %.3f kWh",
                    result.getTotalKwh(), result.getTotalCost(),
                    result.getTotalDayKwh(), result.getTotalNightKwh());

            // Send the response
            sendMessage(chatId, response);

        } catch (Exception e) {
            sendMessage(chatId, "An error occurred while handling the file: " + e.getMessage());
        } finally {
            // Clean up: delete the downloaded file to prevent clutter
            try {
                if (filePath != null) {
                    Files.deleteIfExists(Paths.get(filePath));
                }
            } catch (Exception e) {
                log.error("Failed to delete file: {}", filePath, e);
            }
        }
    }

    public CostCalculationResult calculateTotalCost(List<String[]> data) {
        List<BigDecimal> totalKwhList = new ArrayList<>();
        List<BigDecimal> totalCostList = new ArrayList<>();
        List<BigDecimal> dayCostList = new ArrayList<>();
        List<BigDecimal> nightCostList = new ArrayList<>();
        List<BigDecimal> totalDayKwhList = new ArrayList<>();
        List<BigDecimal> totalNightKwhList = new ArrayList<>();

        BigDecimal fixedSurcharge = new BigDecimal("0.00458");
        BigDecimal monthlyFee = new BigDecimal("1.658");

        BigDecimal dayDistributionFee = new BigDecimal("0.0369");
        BigDecimal nightDistributionFee = new BigDecimal("0.021");
        BigDecimal apartmentMonthlyFee = new BigDecimal("6.39");
        BigDecimal renewableEnergyFee = new BigDecimal("0.0113");
        BigDecimal electricityExciseTax = new BigDecimal("0.001");
        BigDecimal salesTax = new BigDecimal("1.2");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        for (String[] row : data) {
            if (row.length < 4) continue;
            if (row[2].equalsIgnoreCase("kogus (kwh)") || row[2].equalsIgnoreCase("tarbimine")) continue;
            if (row[2].trim().isEmpty()) continue;

            try {
                BigDecimal kwh = new BigDecimal(row[2].replace(',', '.'));
                BigDecimal pricePerMwh = new BigDecimal(row[3].replace(',', '.'));
                BigDecimal pricePerKwh = pricePerMwh.divide(new BigDecimal("1000"), RoundingMode.HALF_UP).add(fixedSurcharge);
                BigDecimal cost = kwh.multiply(pricePerKwh);

                String startTime = row[0].trim();
                LocalDateTime dateTime = LocalDateTime.parse(startTime, formatter);
                int weekday = dateTime.getDayOfWeek().getValue();
                int hour = dateTime.getHour();

                boolean isWeekend = weekday == 6 || weekday == 7; // 6 for Saturday and 7 for Sunday
                boolean isDaytime = (hour >= 7 && hour < 22) && !isWeekend && !isHoliday(dateTime.toLocalDate());
                boolean isNighttime = (hour >= 22 || hour < 7) || isWeekend || isHoliday(dateTime.toLocalDate());

                if (isDaytime) {
                    dayCostList.add(cost);
                    totalDayKwhList.add(kwh);
                } else if (isNighttime) {
                    nightCostList.add(cost);
                    totalNightKwhList.add(kwh);
                }

                totalKwhList.add(kwh);
                totalCostList.add(cost);
            } catch (DateTimeParseException | NumberFormatException e) {
                log.error("Failed to parse row: {}", row, e);
            }
        }

        // Sum up the lists
        BigDecimal totalKwh = totalKwhList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = dayCostList.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(nightCostList.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).add(monthlyFee);
        BigDecimal totalDayKwh = totalDayKwhList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNightKwh = totalNightKwhList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalpriceforday = totalDayKwh.multiply(dayDistributionFee);
        BigDecimal totalpricefornight = totalNightKwh.multiply(nightDistributionFee);

        BigDecimal vorguteenus = totalpriceforday.add(totalpricefornight).add(apartmentMonthlyFee)
                .setScale(2, RoundingMode.HALF_UP);

        // Add monthly fees
        // Apply sales tax at the end
        BigDecimal totalRenewableEnergyFee =
                renewableEnergyFee.multiply(totalKwh)
                        .add(totalKwh.multiply(electricityExciseTax))
                        .setScale(2, RoundingMode.HALF_UP);

        // Add the renewable energy fee to the total cost
        totalCost = totalCost.add(totalRenewableEnergyFee).add(vorguteenus);
        totalCost = totalCost.multiply(salesTax);

        return new CostCalculationResult(totalKwh, totalCost, totalDayKwh, totalNightKwh);
    }


    java.io.File downloadTelegramFile(String fileId) throws TelegramApiException {
        GetFile getFileMethod = new GetFile();
        getFileMethod.setFileId(fileId);
        org.telegram.telegrambots.meta.api.objects.File file = execute(getFileMethod);
        // Using the file path, download the file and return the java.io.File object
        return downloadFile(file);
    }

    public boolean isHoliday(LocalDate date) {
        String formattedDate = date.format(formatter);
        return holidaysConfiguration.getHolidays().contains(formattedDate);
    }

}
