package ee.tenman.elektrihind.electricity;

import ee.tenman.elektrihind.apollo.ApolloKinoService;
import ee.tenman.elektrihind.apollo.ApolloKinoSession;
import ee.tenman.elektrihind.apollo.Option;
import ee.tenman.elektrihind.apollo.SessionManagementService;
import ee.tenman.elektrihind.cache.CacheService;
import ee.tenman.elektrihind.car.CarSearchService;
import ee.tenman.elektrihind.car.PlateDetectionService;
import ee.tenman.elektrihind.car.auto24.Auto24Service;
import ee.tenman.elektrihind.config.FeesConfiguration;
import ee.tenman.elektrihind.config.HolidaysConfiguration;
import ee.tenman.elektrihind.config.ScreenConfiguration;
import ee.tenman.elektrihind.digitalocean.DigitalOceanService;
import ee.tenman.elektrihind.euribor.EuriborRateFetcher;
import ee.tenman.elektrihind.queue.ChatService;
import ee.tenman.elektrihind.queue.OnlineCheckService;
import ee.tenman.elektrihind.telegram.TelegramService;
import ee.tenman.elektrihind.utility.FileToBase64;
import ee.tenman.elektrihind.utility.TextUtility;
import ee.tenman.elektrihind.utility.TimeUtility;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ee.tenman.elektrihind.utility.DateTimeConstants.DATE_TIME_FORMATTER;

@Service
@Slf4j
public class ElectricityBotService extends TelegramLongPollingBot {

    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final Pattern DURATION_PATTERN = Pattern.compile("parim hind (\\d+)(?: h |:)?(\\d+)?(?: min)?", Pattern.CASE_INSENSITIVE);
    public static final Pattern CAR_REGISTRATION_PATTERN = Pattern.compile("^ark\\s+([a-zA-Z0-9]+)$", Pattern.CASE_INSENSITIVE);
    public static final Pattern CAR_PRICE_REGISTRATION_PATTERN = Pattern.compile("^price\\s+([a-zA-Z0-9]+)$", Pattern.CASE_INSENSITIVE);
    public static final Pattern CHAT_PATTERN = Pattern.compile("(?s)^chat\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final String EURIBOR = "euribor";
    private static final String METRIC = "metric";
    private static final String APOLLO_KINO = "apollo_kino";
    private static final Pattern APOLLO_KINO_PATTERN = Pattern.compile(APOLLO_KINO + "=(Täna|Homme|\\d{2}\\.\\d{2}\\.\\d{4})");
    private static final Pattern APOLLO_KINO_UUID_PATTERN = Pattern.compile(APOLLO_KINO + "=([a-f0-9\\-]+)=(.+)");
    private static final Pattern APOLLO_KINO_MOVIE_PATTERN = Pattern.compile(APOLLO_KINO + "=(Täna|Homme|\\d{2}\\.\\d{2}\\.\\d{4})=(.+)");
    private static final Pattern APOLLO_KINO_MOVIE_TIME_PATTERN = Pattern.compile(APOLLO_KINO + "=(Täna|Homme|\\d{2}\\.\\d{2}\\.\\d{4})=(.+)=(.+)");
    private static final Pattern APOLLO_KINO_MOVIE_TIME_ROW_PATTERN = Pattern.compile(APOLLO_KINO + "=(Täna|Homme|\\d{2}\\.\\d{2}\\.\\d{4})=(.+)=(.+)=(.+)");
    private static final Pattern APOLLO_KINO_MOVIE_TIME_ROW_SEAT_PATTERN = Pattern.compile(APOLLO_KINO + "=(Täna|Homme|\\d{2}\\.\\d{2}\\.\\d{4})=(.+)=(.+)=(.+)=(.+)");
    private static final Pattern APOLLO_KINO_MOVIE_TIME_ROW_SEAT_CONFIRM_PATTERN = Pattern.compile(APOLLO_KINO + "=(Täna|Homme|\\d{2}\\.\\d{2}\\.\\d{4})=(.+)=(.+)=(.+)=(.+)=confirm");
    private static final MessageDigest SHA_256_DIGEST;
    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final String REBOOT_COMMAND = "reboot";
    private final ConcurrentHashMap<Integer, AtomicBoolean> messageUpdateFlags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Double> lastPercentages = new ConcurrentHashMap<>();
    private static final int MAX_EDITS_PER_MINUTE = 15;
    private static final long ONE_MINUTE_IN_MILLISECONDS = 60000;
    private final AtomicLong lastEditTimestamp = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger editCount = new AtomicInteger(0);

    static {
        try {
            SHA_256_DIGEST = MessageDigest.getInstance(SHA256_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Resource
    private HolidaysConfiguration holidaysConfiguration;

    @Resource
    private FeesConfiguration feesConfiguration;

    @Resource
    private Clock clock;

    @Resource
    private CacheService cacheService;

    @Resource
    private TelegramService telegramService;

    @Resource
    private PriceFinderService priceFinderService;

    @Resource
    private CarSearchService carSearchService;

    @Resource
    private DigitalOceanService digitalOceanService;

    @Resource
    private PlateDetectionService plateDetectionService;

    @Resource(name = "singleThreadExecutor")
    private ExecutorService singleThreadExecutor;

    @Resource
    private EuriborRateFetcher euriborRateFetcher;

    @Resource
    private ChatService chatService;

    @Resource
    private OnlineCheckService onlineCheckService;

    @Value("${telegram.elektriteemu.token}")
    private String token;

    @Value("${telegram.elektriteemu.username}")
    private String username;

    @Resource
    private Auto24Service auto24Service;

    @Resource
    private ApolloKinoService apolloKinoService;

    @Resource
    private ScreenConfiguration screenConfiguration;

    @Resource
    private SessionManagementService sessionManagementService;

    public static String buildSHA256(String input) {
        byte[] hashInBytes = SHA_256_DIGEST.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : hashInBytes) {
            stringBuilder.append(String.format("%02x", b));
        }
        return stringBuilder.toString();
    }

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

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        AtomicLong startTime = new AtomicLong(System.nanoTime());
        String callData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();

        Matcher arkMatcher = CAR_REGISTRATION_PATTERN.matcher(callData);
        if (arkMatcher.find()) {
            log.info("Received callback query for regNr: {}", arkMatcher.group(1));
            String regNr = arkMatcher.group(1).toUpperCase();
            search(startTime, chatId, regNr, null);
            return;
        }
        Matcher apolloKinoUuidMatcher = APOLLO_KINO_UUID_PATTERN.matcher(callData);
        if (apolloKinoUuidMatcher.find()) {
            log.info("Received callback query for APOLLO_KINO_UUID_PATTERN: {}", apolloKinoUuidMatcher.group(1));
            String uuid = apolloKinoUuidMatcher.group(1);
            String action = apolloKinoUuidMatcher.group(2);
            return;
        }

        Matcher apolloKinoMovieTimeRowSeatConfirmMatcher = APOLLO_KINO_MOVIE_TIME_ROW_SEAT_CONFIRM_PATTERN.matcher(callData);
        if (apolloKinoMovieTimeRowSeatConfirmMatcher.find()) {
            log.info("Received callback query for APOLLO_KINO_MOVIE_TIME_ROW_SEAT_CONFIRM_PATTERN: {}", apolloKinoMovieTimeRowSeatConfirmMatcher.group(1));
            String day = apolloKinoMovieTimeRowSeatConfirmMatcher.group(1);
            String movie = apolloKinoMovieTimeRowSeatConfirmMatcher.group(2);
            String time = apolloKinoMovieTimeRowSeatConfirmMatcher.group(3);
            String row = apolloKinoMovieTimeRowSeatConfirmMatcher.group(4);
            String seat = apolloKinoMovieTimeRowSeatConfirmMatcher.group(5);
            String koht = row + "K" + seat;
            Option.ScreenTime screenTime = apolloKinoService.getOptions().get(day).stream()
                    .filter(screen -> screen.getMovie().equals(movie))
                    .map(Option::getScreenTimes)
                    .flatMap(List::stream)
                    .filter(t -> t.getTime().equals(time))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Screen time not found for " + day + " " + movie + " " + time));

            sendMessageCode(chatId, callbackQuery.getMessage().getMessageId(), "Working on it...");

            Optional<java.io.File> bookedFile = apolloKinoService.book(screenTime, koht);

            if (bookedFile.isPresent()) {
                String confirmationMessage = "Booked " + koht + " for " + movie + ", " + day.toLowerCase() + " kell: " + time;
                confirmationMessage += "\n\nTask duration: " + TimeUtility.durationInSeconds(startTime).asString() + " seconds";
                sendImage(chatId, bookedFile.get());
                sendMessage(chatId, confirmationMessage);
            } else {
                sendMessage(chatId, "Failed to book " + koht + " for " + movie + ", " + day.toLowerCase() + " kell: " + time);
            }
            return;
        }

        Matcher apolloKinoMovieTimeRowSeatMatcher = APOLLO_KINO_MOVIE_TIME_ROW_SEAT_PATTERN.matcher(callData);
        if (apolloKinoMovieTimeRowSeatMatcher.find()) {
            log.info("Received callback query for APOLLO_KINO_MOVIE_TIME_ROW_SEAT_PATTERN: {}", apolloKinoMovieTimeRowSeatMatcher.group(1));
            String day = apolloKinoMovieTimeRowSeatMatcher.group(1);
            String movie = apolloKinoMovieTimeRowSeatMatcher.group(2);
            String time = apolloKinoMovieTimeRowSeatMatcher.group(3);
            String row = apolloKinoMovieTimeRowSeatMatcher.group(4);
            String seat = apolloKinoMovieTimeRowSeatMatcher.group(5);
            displayApolloKinoMenu(chatId, day, movie, time, row, seat);
            return;
        }

        Matcher apolloKinoMovieTimeRowMatcher = APOLLO_KINO_MOVIE_TIME_ROW_PATTERN.matcher(callData);
        if (apolloKinoMovieTimeRowMatcher.find()) {
            log.info("Received callback query for APOLLO_KINO_MOVIE_TIME_ROW_PATTERN: {}", apolloKinoMovieTimeRowMatcher.group(1));
            String day = apolloKinoMovieTimeRowMatcher.group(1);
            String movie = apolloKinoMovieTimeRowMatcher.group(2);
            String time = apolloKinoMovieTimeRowMatcher.group(3);
            String row = apolloKinoMovieTimeRowMatcher.group(4);
            displayApolloKinoMenu(chatId, day, movie, time, row);
            return;
        }

        Matcher apolloKinoMovieTimePatternMatcher = APOLLO_KINO_MOVIE_TIME_PATTERN.matcher(callData);
        if (apolloKinoMovieTimePatternMatcher.find()) {
            log.info("Received callback query for APOLLO_KINO_MOVIE_TIME_PATTERN: {}", apolloKinoMovieTimePatternMatcher.group(1));
            String day = apolloKinoMovieTimePatternMatcher.group(1);
            String movie = apolloKinoMovieTimePatternMatcher.group(2);
            String time = apolloKinoMovieTimePatternMatcher.group(3);
            displayApolloKinoMenu(chatId, day, movie, time);
            return;
        }
        Matcher apolloKinoMoviePatternMatcher = APOLLO_KINO_MOVIE_PATTERN.matcher(callData);
        if (apolloKinoMoviePatternMatcher.find()) {
            log.info("Received callback query for APOLLO_KINO_MOVIE_PATTERN: {}", apolloKinoMoviePatternMatcher.group(1));
            String day = apolloKinoMoviePatternMatcher.group(1);
            String movie = apolloKinoMoviePatternMatcher.group(2);
            displayApolloKinoMenu(chatId, day, movie);
            return;
        }
        Matcher apolloKinoPatternMatcher = APOLLO_KINO_PATTERN.matcher(callData);
        if (apolloKinoPatternMatcher.find()) {
            log.info("Received callback query for APOLLO_KINO_PATTERN: {}", apolloKinoPatternMatcher.group(1));
            String day = apolloKinoPatternMatcher.group(1);
            displayApolloKinoMenu(chatId, day);
            return;
        }

        switch (callData) {
            case "check_price" -> sendMessageCode(chatId, getElectricityPriceResponse());
            case "car_plate_query" -> sendMessage(chatId, "Please enter the car plate number with the 'ark' command.");
            case EURIBOR -> sendMessageCode(chatId, euriborRateFetcher.getEuriborRateResponse());
            case METRIC -> sendMessageCode(chatId, getSystemMetrics());
            case APOLLO_KINO -> {
                ApolloKinoSession newSession = sessionManagementService.createNewSession();
//                displayApolloKinoMenu(chatId, newSession);
                displayApolloKinoMenu(chatId);
            }
            case REBOOT_COMMAND -> {
                digitalOceanService.rebootDroplet();
                sendMessageCode(chatId, "Droplet reboot initiated!");
            }
            case "automaticFetching true" -> {
                cacheService.setAutomaticFetchingEnabled(true);
                sendMessage(chatId, "Automatic fetching enabled.");
            }
            case "automaticFetching false" -> {
                cacheService.setAutomaticFetchingEnabled(false);
                sendMessage(chatId, "Automatic fetching disabled.");
            }
            case "automaks true" -> {
                cacheService.setAutomaksEnabled(true);
                sendMessage(chatId, "Automaks enabled.");
            }
            case "automaks false" -> {
                cacheService.setAutomaksEnabled(false);
                sendMessage(chatId, "Automaks disabled.");
            }
            default -> sendMessage(chatId, "Command not recognized.");
        }
    }

    private void displayApolloKinoMenu(long chatId, ApolloKinoSession newSession) {

    }

    private void displayApolloKinoMenu(long chatId, String day, String movie, String time, String row, String seat) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        Option.ScreenTime screenTime = apolloKinoService.getOptions().get(day).stream()
                .filter(screen -> screen.getMovie().equals(movie))
                .map(Option::getScreenTimes)
                .flatMap(List::stream)
                .filter(t -> t.getTime().equals(time))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Screen time not found for " + day + " " + movie + " " + time));


        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        String koht = row + "K" + seat;
        InlineKeyboardButton confirm = new InlineKeyboardButton("Confirm");
        confirm.setCallbackData(APOLLO_KINO + "=" + day + "=" + movie + "=" + time + "=" + row + "=" + seat + "=confirm");
        rowInline.add(confirm);

        rowsInline.add(rowInline);

        markupInline.setKeyboard(rowsInline);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Book: '" + koht + "' for " + movie + ", " + day.toLowerCase() + " kell: " + time + "?");
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to display apollo kino menu with times: {}", e.getMessage());
        }

    }

    private void displayApolloKinoMenu(long chatId, String day, String movie, String time, String row) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        Option.ScreenTime screenTime = apolloKinoService.getOptions().get(day).stream()
                .filter(screen -> screen.getMovie().equals(movie))
                .map(Option::getScreenTimes)
                .flatMap(List::stream)
                .filter(t -> t.getTime().equals(time))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Screen time not found for " + day + " " + movie + " " + time));

        int maxSeats = screenConfiguration.getScreen(screenTime.getHall()).getSeatCounts().get(row);

        for (int i = 1; i <= maxSeats; i++) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton(i + "");
            button.setCallbackData(APOLLO_KINO + "=" + day + "=" + movie + "=" + time + "=" + row + "=" + i);
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        markupInline.setKeyboard(rowsInline);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(String.format("Select a seat in row %s :", row));
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to display apollo kino menu with times: {}", e.getMessage());
        }
    }

    private void displayApolloKinoMenu(long chatId, String day, String movie, String time) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        Option.ScreenTime screenTime = apolloKinoService.getOptions().get(day).stream()
                .filter(screen -> screen.getMovie().equals(movie))
                .map(Option::getScreenTimes)
                .flatMap(List::stream)
                .filter(t -> t.getTime().equals(time))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Screen time not found for " + day + " " + movie + " " + time));

        Map<String, Integer> seatCounts = screenConfiguration.getScreen(screenTime.getHall()).getSeatCounts();

        for (Map.Entry<String, Integer> entry : seatCounts.entrySet()) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton(entry.getKey());
            button.setCallbackData(APOLLO_KINO + "=" + day + "=" + movie + "=" + time + "=" + entry.getKey());
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        markupInline.setKeyboard(rowsInline);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Select a row to book:");
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to display apollo kino menu with times: {}", e.getMessage());
        }
    }

    private void displayApolloKinoMenu(long chatId, String day, String movie) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        apolloKinoService.getOptions().get(day).stream()
                .filter(screen -> screen.getMovie().equals(movie))
                .map(Option::getScreenTimes)
                .flatMap(List::stream)
                .forEach(t -> {
                    InlineKeyboardButton button = new InlineKeyboardButton(t.getTime());
                    button.setCallbackData(APOLLO_KINO + "=" + day + "=" + movie + "=" + t.getTime());
                    rowInline.add(button);
                });

        rowsInline.add(rowInline);

        markupInline.setKeyboard(rowsInline);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Select a time:");
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to display apollo kino menu with times: {}", e.getMessage());
        }
    }

    private void displayApolloKinoMenu(long chatId, String day) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        List<Option> optionsList = apolloKinoService.getOptions().get(day);
        String title = "";
        for (int i = 0; i < optionsList.size(); i++) {
            Option screen = optionsList.get(i);
            title += screen.getMovie() + (i + 1 < optionsList.size() ? optionsList.get(i + 1).getMovie() : "");
            InlineKeyboardButton button = new InlineKeyboardButton(screen.getMovie());
            button.setCallbackData(APOLLO_KINO + "=" + day + "=" + screen.getMovie());
            rowInline.add(button);
            if (i % 2 == 1 || title.length() > 40) {
                rowsInline.add(rowInline);
                rowInline = new ArrayList<>();
                title = "";
            }
        }

        if (rowInline.isEmpty()) {
            sendMessage(chatId, "No movies found for " + day);
            return;
        }

        rowsInline.add(rowInline);

        markupInline.setKeyboard(rowsInline);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Select a movie:");
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to apollo kino menu with options: {}", e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        } else if (update.hasMessage()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();

            if (message.hasText()) {
                handleTextMessage(message);
            } else if (message.hasDocument()) {
                handleDocumentMessage(message, chatId);
            } else if (message.hasPhoto()) {
                handlePhotoMessage(message, chatId);
            }
        }
    }

    private void handlePhotoMessage(Message message, long chatId) {
        // Photos are sent as a list, the highest quality photo is usually the last one
        List<PhotoSize> photos = message.getPhoto();
        if (photos == null || photos.isEmpty()) {
            sendMessage(chatId, "No photo detected.");
            return;
        }

        String fileId = photos.getLast().getFileId();
        byte[] imageBytes = downloadImage(fileId); // Implement downloadImage to retrieve the photo as byte array
        handlePlateNumberImage(message, imageBytes);
    }

    private void handlePlateNumberImage(Message message, byte[] imageBytes) {
        AtomicLong startTime = new AtomicLong(System.nanoTime());
        String base64EncodedImage = FileToBase64.encodeToBase64(imageBytes);
        String imageHashValue = buildSHA256(base64EncodedImage);
        Optional<String> detectedPlate = plateDetectionService.detectPlate(base64EncodedImage, imageHashValue);

        if (detectedPlate.isEmpty()) {
            return;
        }

        String plateNumber = detectedPlate.get();
        InlineKeyboardMarkup inlineKeyboardMarkup = createInlineKeyboardForPlateNumber(plateNumber);

        String messageContent = cacheService.isAutomaticFetchingEnabled() ?
                "Detected a plate number: " + plateNumber :
                "Detected a potential plate number. Would you like to check it?";

        SendMessage messageWithButton = createMessageWithInlineKeyboard(message, messageContent, inlineKeyboardMarkup);
        if (!cacheService.isAutomaticFetchingEnabled()) {
            executeSendMessage(messageWithButton);
        }
        performSearchIfAutoFetchingEnabled(startTime, message, plateNumber);
    }

    private void performSearchIfAutoFetchingEnabled(AtomicLong startTime, Message message, String plateNumber) {
        if (!cacheService.isAutomaticFetchingEnabled()) {
            return;
        }
        search(startTime, message.getChatId(), plateNumber, message.getMessageId());
    }

    private void displayApolloKinoMenu(long chatId) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        apolloKinoService.getOptions().keySet().forEach(option -> {
            InlineKeyboardButton button = new InlineKeyboardButton(option);
            button.setCallbackData(APOLLO_KINO + "=" + option);
            rowInline.add(button);
        });

        rowsInline.add(rowInline);

        markupInline.setKeyboard(rowsInline);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Select an option:");
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to apollo kino menu: {}", e.getMessage());
        }
    }

    private void displayMenu(long chatId) {
        ensureEditLimit();
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Existing buttons
        InlineKeyboardButton buttonCheckPrice = new InlineKeyboardButton("Elektrihind");
        buttonCheckPrice.setCallbackData("check_price");

        InlineKeyboardButton buttonCarPlateQuery = new InlineKeyboardButton("Car Plate Query");
        buttonCarPlateQuery.setCallbackData("car_plate_query");

        // New buttons
        InlineKeyboardButton buttonEuribor = new InlineKeyboardButton("Euribor");
        buttonEuribor.setCallbackData(EURIBOR);

        InlineKeyboardButton buttonMetric = new InlineKeyboardButton("Metrics");
        buttonMetric.setCallbackData(METRIC);

        InlineKeyboardButton buttonReboot = new InlineKeyboardButton("Reboot");
        buttonReboot.setCallbackData(REBOOT_COMMAND);

        // Adding buttons to the keyboard
        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        rowInline1.add(buttonCheckPrice);
        rowInline1.add(buttonCarPlateQuery);

        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        rowInline2.add(buttonEuribor);
        rowInline2.add(buttonMetric);
        rowInline2.add(buttonReboot);

        List<InlineKeyboardButton> rowInline3 = new ArrayList<>();
        InlineKeyboardButton autoMaticFetchingEnablingButton = getAutoMaticFetchingEnablingButton();
        rowInline3.add(autoMaticFetchingEnablingButton);

        List<InlineKeyboardButton> rowInline4 = new ArrayList<>();
        InlineKeyboardButton automaksEnablingButton = getAutomaksEnablingButton();
        rowInline4.add(automaksEnablingButton);

        List<InlineKeyboardButton> rowInline5 = new ArrayList<>();
        InlineKeyboardButton apolloKino = new InlineKeyboardButton("Apollo Kino");
        apolloKino.setCallbackData(APOLLO_KINO);
        rowInline5.add(apolloKino);

        // Set the keyboard to the markup
        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);
        rowsInline.add(rowInline3);
        rowsInline.add(rowInline4);
        rowsInline.add(rowInline5);
        markupInline.setKeyboard(rowsInline);

        // Creating a message and setting the markup
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Select an option:");
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to display menu: {}", e.getMessage());
        }
    }

    private InlineKeyboardButton getAutoMaticFetchingEnablingButton() {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        boolean automaticFetchingEnabled = cacheService.isAutomaticFetchingEnabled();
        inlineKeyboardButton.setText(automaticFetchingEnabled ? "Disable automatic fetching" : "Enable automatic fetching");
        inlineKeyboardButton.setCallbackData("automaticFetching " + !automaticFetchingEnabled);
        return inlineKeyboardButton;
    }

    private InlineKeyboardButton getAutomaksEnablingButton() {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        boolean isAutomaksEnabled = cacheService.isAutomaksEnabled();
        inlineKeyboardButton.setText(isAutomaksEnabled ? "Disable automaks" : "Enable automaks");
        inlineKeyboardButton.setCallbackData("automaks " + !isAutomaksEnabled);
        return inlineKeyboardButton;
    }

    private void handleTextMessage(Message message) {
        AtomicLong startTime = new AtomicLong(System.nanoTime());
        long chatId = message.getChatId();
        int messageId = message.getMessageId();

        String messageText = message.getText();
        Matcher matcher = DURATION_PATTERN.matcher(messageText);
        Matcher arkMatcher = CAR_REGISTRATION_PATTERN.matcher(messageText);
        Matcher chatMatcher = CHAT_PATTERN.matcher(messageText);
        Matcher carPriceMatcher = CAR_PRICE_REGISTRATION_PATTERN.matcher(messageText);

        boolean showMenu = false;
        if ("/start".equalsIgnoreCase(messageText)) {
            sendMessage(chatId, "Welcome to the bot!");
            showMenu = true;
        } else if ("/menu".equalsIgnoreCase(messageText)) {
            showMenu = true;
        } else if (messageText.toLowerCase().contains("elektrihind")) {
            String response = getElectricityPriceResponse();
            sendMessageCode(chatId, messageId, response);
        } else if (messageText.toLowerCase().contains(METRIC) || messageText.toLowerCase().contains("/" + METRIC)) {
            String response = getSystemMetrics();
            sendMessageCode(chatId, messageId, response);
        } else if (messageText.toLowerCase().contains(EURIBOR)) {
            String euriborResonse = euriborRateFetcher.getEuriborRateResponse();
            sendMessageCode(chatId, messageId, euriborResonse);
        } else if (arkMatcher.find()) {
            String regNr = arkMatcher.group(1).toUpperCase();
            search(startTime, chatId, regNr, messageId);
        } else if (carPriceMatcher.find()) {
            String regNr = carPriceMatcher.group(1).toUpperCase();
            price(startTime, chatId, regNr, messageId);
        } else if (chatMatcher.find()) {
            String text = chatMatcher.group(1);
            String response = onlineCheckService.isMacbookOnline() ? chatService.sendMessage(text)
                    .map(t -> t + "\n\nTask duration: " + TimeUtility.durationInSeconds(startTime).asString() + " seconds")
                    .orElse("Response timeout or Macbook is sleeping.") : "Macbook is offline.";
            sendMessageCode(chatId, messageId, response);
        } else if (messageText.equalsIgnoreCase(REBOOT_COMMAND)) {
            digitalOceanService.rebootDroplet();
            sendMessageCode(chatId, messageId, "Droplet reboot initiated!");
        } else if (matcher.find()) {
            handleDurationMessage(matcher, chatId, messageId);
        } // Consider adding an else block for unhandled text messages
        if (showMenu) {
            displayMenu(chatId);
        }
    }

    private void price(AtomicLong startTime, long chatId, String regNr, Integer originalMessageId) {
        if (startTime.get() == 0) {
            startTime.set(System.nanoTime());
        }

        Message message = sendMessageCode(chatId, originalMessageId, "Fetching car price for registration plate " + regNr + "...");
        Integer messageId = message.getMessageId();
        messageUpdateFlags.put(messageId, new AtomicBoolean(false));
        beginMessageUpdateAnimation(chatId, regNr, messageId, "price");

        CompletableFuture.runAsync(() -> {
                    startTime.set(System.nanoTime());
                    Map<String, String> carSearchData = auto24Service.carPrice(regNr);
                    handleCarSearchUpdate(chatId, carSearchData, true, messageId, startTime, "price");
                }, singleThreadExecutor)
                .orTimeout(15, TimeUnit.MINUTES)
                .exceptionally(throwable -> {
                    if (throwable.getCause() instanceof TimeoutException) {
                        log.error("Fetching car price timed out for regNr: {}", throwable.getMessage());
                        editMessage(chatId, messageId, "An error occurred while fetching car details for: " + regNr);
                    } else {
                        log.error("Error fetching car details: {}", throwable.getLocalizedMessage());
                        editMessage(chatId, messageId, "Fetching car price timed out for: " + regNr);
                    }
                    messageUpdateFlags.remove(messageId);
                    lastPercentages.remove(messageId);
                    return null;
                });
    }

    private void search(AtomicLong startTime, long chatId, String regNr, Integer originalMessageId) {
        if (startTime.get() == 0) {
            startTime.set(System.nanoTime());
        }

        Message message = sendMessageCode(chatId, originalMessageId, "Fetching car details for registration plate " + regNr + "...");
        Integer messageId = message.getMessageId();
        messageUpdateFlags.put(messageId, new AtomicBoolean(false));
        beginMessageUpdateAnimation(chatId, regNr, messageId);

        CompletableFuture.runAsync(() -> {
                    startTime.set(System.nanoTime());
                    CarSearchUpdateListener listener = (data, isFinalUpdate) -> handleCarSearchUpdate(chatId, data, isFinalUpdate, messageId, startTime);
                    Map<String, String> carSearchData = carSearchService.searchV2(regNr, listener);
                    handleCarSearchUpdate(chatId, carSearchData, true, messageId, startTime);
                }, singleThreadExecutor)
                .orTimeout(15, TimeUnit.MINUTES)
                .exceptionally(throwable -> {
                    if (throwable.getCause() instanceof TimeoutException) {
                        log.error("Fetching car details timed out for regNr: {}", throwable.getMessage());
                        sendMessageWithRetryButton(chatId, "An error occurred while fetching car details.", regNr);
                    } else {
                        log.error("Error fetching car details: {}", throwable.getLocalizedMessage());
                        sendMessageWithRetryButton(chatId, "Fetching car details timed out. Click below to retry.", regNr);
                    }
                    messageUpdateFlags.remove(messageId);
                    lastPercentages.remove(messageId);
                    return null;
                });
    }

    private void beginMessageUpdateAnimation(long chatId, String regNr, Integer messageId, String text) {
        new Thread(() -> {
            try {
                int timeout = randomTimeout();
                int count = -1;
                double lastPercentage = 0;
                double averageDuration = getMedianDuration();
                double timeTaken = timeout + 0.0000000000001;
                while (messageUpdateFlags.get(messageId) != null && !messageUpdateFlags.get(messageId).get()) {
                    TimeUnit.SECONDS.sleep(timeout);
                    if (messageUpdateFlags.get(messageId) != null && !messageUpdateFlags.get(messageId).get()) {
                        double percentage = averageDuration != 0 ? (timeTaken / averageDuration) * 100 : 0;
                        String suffix = "";
                        if (percentage > 0 && timeTaken < averageDuration) {
                            suffix = " (" + BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP) + "%)";
                            lastPercentage = percentage;
                        } else if (percentage > 0 && timeTaken > averageDuration) {
                            suffix = " (" + BigDecimal.valueOf(lastPercentage).setScale(2, RoundingMode.HALF_UP) + "%)";
                            if (lastPercentage < 100) {
                                lastPercentage = lastPercentage + randomIncrement();
                            }
                        } else if (percentage == 0 && averageDuration != 0) {
                            suffix = " (0.00%)";
                        }
                        editMessage(chatId, messageId, "Fetching car " + text + " for registration plate " + regNr + "..." + ".".repeat(++count) + getArrow(count) + suffix);
                    }
                    timeout = randomTimeout();
                    timeTaken = timeTaken + timeout;
                }
                lastPercentages.put(messageId, lastPercentage);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Message updating thread interrupted", e);
            }
        }).start();
    }


    private void beginMessageUpdateAnimation(long chatId, String regNr, Integer messageId) {
        beginMessageUpdateAnimation(chatId, regNr, messageId, "details");
    }

    private double randomIncrement() {
        double[] numbers = {0.01, 0.02, 0.03, 0.04, 0.05};
        int randomIndex = RANDOM.nextInt(numbers.length);
        return numbers[randomIndex];
    }

    private int randomTimeout() {
        int[] numbers = {2, 3, 4};
        int randomIndex = RANDOM.nextInt(numbers.length);
        return numbers[randomIndex];
    }

    private double getMedianDuration() {
        List<Double> sortedValues = cacheService.getDurations().stream().sorted().toList();
        double median;
        int size = sortedValues.size();
        if (size > 0) {
            if (size % 2 == 0) {
                median = (sortedValues.get(size / 2 - 1) + sortedValues.get(size / 2)) / 2.0;
            } else {
                median = sortedValues.get(size / 2);
            }
        } else {
            median = 0;
        }
        return median;
    }

    private String getArrow(int count) {
        return switch (count % 8) {
            case 1 -> "↘";
            case 2 -> "↓";
            case 3 -> "↙";
            case 4 -> "←";
            case 5 -> "↖";
            case 6 -> "↑";
            case 7 -> "↗";
            default -> "→";
        };
    }

    private void ensureEditLimit() {
        boolean shouldContinue = true;
        while (shouldContinue) {
            if (hasOneMinutePassedSinceLastEdit()) {
                resetEditCount();
                shouldContinue = false;
            } else if (editCount.get() < MAX_EDITS_PER_MINUTE) {
                shouldContinue = false;
            } else {
                pauseThreadDueToRateLimit();
            }
        }
    }

    private boolean hasOneMinutePassedSinceLastEdit() {
        return System.currentTimeMillis() - lastEditTimestamp.get() > ONE_MINUTE_IN_MILLISECONDS;
    }

    private void resetEditCount() {
        long currentTime = System.currentTimeMillis();
        editCount.set(0);
        lastEditTimestamp.set(currentTime);
    }

    private void pauseThreadDueToRateLimit() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for rate limit", e);
        }
    }

    private synchronized void editMessage(long chatId, int messageId, String newText) {
        ensureEditLimit();

        log.info("Editing message in chat: {} with new text: {}", chatId, newText);
        if (newText == null) {
            log.warn("Not editing message in chat: {} as the new text is null", chatId);
            return;
        }

        // MarkdownV2 format adds extra characters for backticks and new lines
        String messageText = "```\n" + newText + "\n```";

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setMessageId(messageId);
        editMessage.setText(messageText);
        editMessage.setParseMode("MarkdownV2");
        editMessage.enableMarkdown(true);

        try {
            execute(editMessage);
            editCount.incrementAndGet(); // Increment the counter after a successful edit
        } catch (TelegramApiException e) {
            log.error("Failed to edit message in chat: {} with new text: {}", chatId, newText, e);
        }
    }

    private void stopMessageUpdate(int messageId) {
        AtomicBoolean flag = messageUpdateFlags.get(messageId);
        if (flag != null) {
            flag.set(true);
        }
    }

    private void handleCarSearchUpdate(long chatId, Map<String, String> carDetails, boolean isFinalUpdate, Integer messageId, AtomicLong startTime, String text) {
        stopMessageUpdate(messageId);
        String updateText = formatCarSearchData(carDetails);

        double averageDuration = getMedianDuration();
        double timeTaken = TimeUtility.durationInSeconds(startTime).asDouble();
        double percentage = averageDuration != 0 ? (timeTaken / averageDuration) * 100 : 0;
        String suffix = "";
        if (percentage > 0 && timeTaken < averageDuration) {
            suffix = " (" + BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP) + "%)";
            lastPercentages.put(messageId, percentage);
        } else if (percentage > 0 && timeTaken > averageDuration) {
            double lastPercentage = Optional.ofNullable(lastPercentages.get(messageId)).orElse(0.0);
            suffix = " (" + BigDecimal.valueOf(lastPercentage).setScale(2, RoundingMode.HALF_UP) + "%)";
            if (lastPercentage < 100) {
                lastPercentage = lastPercentage + randomIncrement();
                lastPercentages.put(messageId, lastPercentage);
            }
        } else if (percentage == 0 && averageDuration != 0) {
            suffix = " (0.00%)";
        }

        try {
            if (isFinalUpdate) {
                TimeUtility.CustomDuration duration = TimeUtility.durationInSeconds(startTime);
                editMessage(chatId, messageId, updateText + "\n\nTask duration: " + duration.asString() + " seconds");
                messageUpdateFlags.remove(messageId);
                double animationDuration = duration.asDouble();
                if (animationDuration > 15 && animationDuration < 240 && carDetails.size() > 2) {
                    cacheService.addDuration(animationDuration);
                    log.info("Added animationDuration: {}", animationDuration);
                }
                if (carDetails.size() == 2 && !carDetails.containsKey("Turuhind")) {
                    String regNr = carDetails.get("Reg nr");
                    sendMessageWithRetryButton(chatId, "Not enough car " + text + " found for registration plate. Click below to retry.", regNr);
                }
            } else {
                updateText = updateText + suffix;
                editMessage(chatId, messageId, updateText);
            }
        } catch (Exception e) {
            log.error("Error handling car search update: {}", e.getMessage());
        }
    }

    private void handleCarSearchUpdate(long chatId, Map<String, String> carDetails, boolean isFinalUpdate, Integer messageId, AtomicLong startTime) {
        handleCarSearchUpdate(chatId, carDetails, isFinalUpdate, messageId, startTime, "details");
    }

    private String formatCarSearchData(Map<String, String> data) {
        return data.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }


    private void sendMessageWithRetryButton(long chatId, String text, String regNr) {
        cacheService.evictCacheEntry(regNr);
        ensureEditLimit();
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        InlineKeyboardButton retryButton = new InlineKeyboardButton();
        retryButton.setText("Rerun 'ark " + regNr + "'");
        retryButton.setCallbackData("ark " + regNr); // Ensure this callback is handled in handleCallbackQuery method

        rowInline.add(retryButton);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);

        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send timeout message with retry button: {}", e.getMessage());
        }
    }

    private String getSystemMetrics() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();

        // CPU Usage
        CentralProcessor processor = hal.getProcessor();
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        // Wait a second to get a better measurement
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100; // Get system CPU load between ticks

        // Memory Usage
        GlobalMemory memory = hal.getMemory();
        double memoryUsage = (double) (memory.getTotal() - memory.getAvailable()) / memory.getTotal() * 100;

        // Disk Usage
        FileSystem fileSystem = si.getOperatingSystem().getFileSystem();
        List<OSFileStore> fsList = fileSystem.getFileStores();
        long totalSpace = 0;
        long usableSpace = 0;
        for (OSFileStore fs : fsList) {
            totalSpace += fs.getTotalSpace();
            usableSpace += fs.getTotalSpace() - fs.getUsableSpace();
        }
        double diskUsage = 0.0;
        if (totalSpace > 0) {
            diskUsage = (double) usableSpace / totalSpace * 100;
        }

        return String.format("CPU: %.2f %% %n" +
                "Disk Usage: %.2f %% %n" +
                "Memory Usage: %.2f %%", cpuLoad, diskUsage, memoryUsage);
    }

    private void handleDocumentMessage(Message message, long chatId) {
        Document document = message.getDocument();
        if (document == null || document.getFileName() == null) {
            sendMessage(chatId, "No file detected.");
            return;
        }

        String fileName = document.getFileName();
        if (fileName.endsWith(".csv")) {
            handleCsvDocument(document, chatId);
            return;
        }

        if (fileName.toLowerCase().matches(".*(\\.jpg|\\.png)")) {
            byte[] imageBytes = downloadImage(document.getFileId());
            handlePlateNumberImage(message, imageBytes);
            return;
        }

        sendMessage(chatId, "Please send a CSV or image file.");
    }

    @SneakyThrows
    private byte[] downloadImage(String fileId) {
        // Use getFile to get the file path
        GetFile getFileMethod = new GetFile();
        getFileMethod.setFileId(fileId);
        File file = execute(getFileMethod);

        // Use the file path to download the file
        String filePath = file.getFilePath();
        String fileUrl = "https://api.telegram.org/file/bot" + token + "/" + filePath;
        return downloadFileAsBytes(fileUrl); // Implement this method to download the file from the URL
    }

    private byte[] downloadFileAsBytes(String fileUrl) throws IOException {
        URL url = new URL(fileUrl);
        try (InputStream in = url.openStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return out.toByteArray();
        }
    }

    private void executeSendMessage(SendMessage message) {
        ensureEditLimit();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup createInlineKeyboardForPlateNumber(String plateNumber) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();

        InlineKeyboardButton buttonCheckPlate = new InlineKeyboardButton();
        buttonCheckPlate.setText("Check car plate " + plateNumber);
        buttonCheckPlate.setCallbackData("ark " + plateNumber);

        InlineKeyboardButton autoMaticFetchingEnablingButton = getAutoMaticFetchingEnablingButton();

        rowInline1.add(buttonCheckPlate);
        rowInline2.add(autoMaticFetchingEnablingButton);

        if (!cacheService.isAutomaticFetchingEnabled()) {
            rowsInline.add(rowInline1);
        }

        rowsInline.add(rowInline2);

        markupInline.setKeyboard(rowsInline);

        return markupInline;
    }

    private SendMessage createMessageWithInlineKeyboard(Message message, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(text);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());
        return sendMessage;
    }

    String getElectricityPriceResponse() {
        List<ElectricityPrice> electricityPrices = cacheService.getLatestPrices();
        Optional<ElectricityPrice> currentPrice = priceFinderService.currentPrice(electricityPrices);

        if (currentPrice.isEmpty()) {
            log.warn("Could not find current electricity price.");
            return null;
        }

        StringBuilder response = new StringBuilder("""
                Current electricity price is %s cents/kWh.%n
                """.formatted(currentPrice.map(ElectricityPrice::getPrice).orElseThrow()));

        LocalDateTime now = LocalDateTime.now(clock);
        TreeSet<ElectricityPrice> upcomingPrices = electricityPrices.stream()
                .filter(Objects::nonNull)
                .filter(price -> price.getDate().isAfter(now))
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(ElectricityPrice::getDate))));

        if (upcomingPrices.isEmpty()) {
            response.append("No upcoming price data available.");
        } else {
            response.append("Upcoming prices:\n");
            response.append(telegramService.formatPricesForTelegram(upcomingPrices));
        }
        return response.toString();
    }

    private void handleDurationMessage(Matcher matcher, long chatId, int messageId) {
        LocalDateTime now = LocalDateTime.now(clock);
        int durationInMinutes = durationInMinutes(matcher);
        List<ElectricityPrice> latestPrices = cacheService.getLatestPrices();
        List<ElectricityPrice> electricityPrices = latestPrices
                .stream()
                .filter(electricityPrice -> {
                    LocalDateTime priceDate = electricityPrice.getDate();
                    LocalDateTime startOfCurrentHour = now.truncatedTo(ChronoUnit.HOURS);
                    return !priceDate.isBefore(startOfCurrentHour);
                })
                .toList();
        BestPriceResult bestPrice = priceFinderService.findBestPriceForDuration(electricityPrices, durationInMinutes);

        if (bestPrice == null) {
            sendMessageCode(chatId, messageId, "Could not calculate the best time to start your washing machine.");
            return;
        }

        BigDecimal calculatedImmediateCost = priceFinderService.calculateImmediateCost(latestPrices, durationInMinutes);
        BestPriceResult currentBestPriceResult = new BestPriceResult(now, calculatedImmediateCost.doubleValue(), durationInMinutes);

        String response = formatBestPriceResponse(bestPrice);
        response += formatBestPriceResponseForCurrent(currentBestPriceResult);
        String difference = String.format(" %.2f", currentBestPriceResult.getTotalCost() / bestPrice.getTotalCost()) + "x more expensive to start immediately.";
        response += difference;

        sendMessageCode(chatId, messageId, response);
    }

    private String formatBestPriceResponseForCurrent(BestPriceResult currentBestPriceResult) {
        return "\n\nStart consuming immediately at " + LocalDateTime.now(clock).format(DATE_TIME_FORMATTER) + ". " +
                "Total cost is " + currentBestPriceResult.getTotalCost() + " cents with average price of " + currentBestPriceResult.getAveragePrice() + " cents/kWh.";
    }

    int durationInMinutes(Matcher matcher) {
        int hours = 0;
        int minutes;

        int firstNumber = Integer.parseInt(matcher.group(1));
        if (matcher.group(2) != null) {
            hours = firstNumber;
            minutes = Integer.parseInt(matcher.group(2));
        } else {
            minutes = firstNumber;
        }
        return hours * 60 + minutes;
    }

    String formatBestPriceResponse(BestPriceResult bestPrice) {
        return "Best time to start is " + bestPrice.getStartTime().format(DATE_TIME_FORMATTER) +
                " with average price of " + bestPrice.getAveragePrice() + " cents/kWh. " +
                "Total cost is " + bestPrice.getTotalCost() + " cents. In " +
                Duration.between(LocalDateTime.now(clock), bestPrice.getStartTime()).toHours() + " hours!";
    }

    Message sendMessage(long chatId, String text) {
        ensureEditLimit();
        if (text == null) {
            log.warn("Not sending null message to chat: {}", chatId);
            return null;
        }
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            log.info("Sending message to chat: {} with text: {}", chatId, text);
            return execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message: {}", text, e);
            return null;
        }
    }

    void sendImage(long chatId, String imageUrl) {
        ensureEditLimit();
        SendPhoto sendPhotoRequest = new SendPhoto();
        sendPhotoRequest.setChatId(String.valueOf(chatId));
        sendPhotoRequest.setPhoto(new InputFile(imageUrl)); // Set the URL or file path
        try {
            execute(sendPhotoRequest);
        } catch (TelegramApiException e) {
            log.error("Failed to send image to chat: {} with URL: {}", chatId, imageUrl, e);
        }
    }

    private void sendImage(long chatId, java.io.File file) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile(file));
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error("Failed to send image: {}", e.getMessage());
        }
    }

    Message sendMessageCode(long chatId, Integer replyToMessageId, String text) {
        ensureEditLimit();
        if (text == null) {
            log.warn("Not sending null message to chat: {}", chatId);
            return null;
        }
        text = TextUtility.escapeMarkdown(text);

        // MarkdownV2 format adds extra characters for backticks and new lines
        int maxTextLength = 4096 - 8; // Accounting for triple backticks and new lines

        int start = 0;
        boolean isFirstMessage = true;
        Message lastMessage = null;
        while (start < text.length()) {
            int end = Math.min(start + maxTextLength, text.length());
            String chunk = text.substring(start, end);

            SendMessage message = new SendMessage();
            message.setParseMode("MarkdownV2");
            message.enableMarkdown(true);
            message.enableMarkdownV2(true);
            message.setChatId(String.valueOf(chatId));
            if (isFirstMessage && replyToMessageId != null) {
                message.setReplyToMessageId(replyToMessageId);
            }

            String messageText = "```\n" + chunk + "\n```";
            message.setText(messageText);

            try {
                lastMessage = execute(message);
            } catch (TelegramApiException e) {
                log.error("Failed to send message to chat: {} with text: {}", chatId, chunk, e);
            }

            isFirstMessage = false;
            start = end;
        }
        return lastMessage;
    }

    public void sendMessageCode(long chatId, String text) {
        ensureEditLimit();
        if (text == null) {
            log.warn("Not sending null message to chat: {}", chatId);
            return;
        }
        text = TextUtility.escapeMarkdown(text);

        SendMessage message = new SendMessage();
        message.setParseMode("MarkdownV2");
        message.enableMarkdown(true);
        message.enableMarkdownV2(true);
        message.setChatId(String.valueOf(chatId));
        message.setText("```\n" + text + "```");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat: {} with text: {}", chatId, text, e);
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

        BigDecimal fixedSurcharge = feesConfiguration.getFixedSurcharge();
        BigDecimal monthlyFee = feesConfiguration.getMonthlyFee();

        BigDecimal dayDistributionFee = feesConfiguration.getDayDistributionFee();
        BigDecimal nightDistributionFee = feesConfiguration.getNightDistributionFee();
        BigDecimal apartmentMonthlyFee = feesConfiguration.getApartmentMonthlyFee();
        BigDecimal renewableEnergyFee = feesConfiguration.getRenewableEnergyFee();
        BigDecimal electricityExciseTax = feesConfiguration.getElectricityExciseTax();
        BigDecimal salesTax = feesConfiguration.getSalesTax();

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
                boolean isDaytime = isDayTime(dateTime);
                boolean isNighttime = isNighttime(dateTime);

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
        File file = execute(getFileMethod);
        // Using the file path, download the file and return the java.io.File object
        return downloadFile(file);
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
