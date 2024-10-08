package ee.tenman.elektrihind.apollo;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import ee.tenman.elektrihind.cache.CacheService;
import ee.tenman.elektrihind.config.ScreenConfiguration;
import ee.tenman.elektrihind.movies.MovieDetails;
import ee.tenman.elektrihind.movies.MovieDetailsService;
import ee.tenman.elektrihind.utility.AsyncRunner;
import ee.tenman.elektrihind.utility.TimeUtility;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.sleep;
import static com.codeborne.selenide.Selenide.switchTo;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static ee.tenman.elektrihind.utility.GlobalConstants.TEST_PROFILE;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.name;
import static org.openqa.selenium.By.tagName;

@Service
@Slf4j
public class ApolloKinoService {
    
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private Map<Cinema, Map<LocalDate, List<Option>>> options = new TreeMap<>();
    
    @Value("${apollo-kino.username}")
    private String username;
    @Value("${apollo-kino.password}")
    private String password;
    
    @Resource
    private ScreenConfiguration screenConfig;
    @Resource
    private Environment environment;
    @Resource
    private CacheService cacheService;
    @Resource
    private MovieDetailsService movieDetailsService;
    @Resource
    private AsyncRunner asyncRunner;
    
    private static void selectSeats(int count) {
        $$(".radio-card__text").find(text("Staaritoolid")).click();
        sleep(777);
        for (int i = 0; i < 10; i++) {
            SelenideElement subtractButton = $$(".number__button--subtract ").first();
            if (subtractButton.has(attribute("disabled"))) {
                break;
            }
            subtractButton.click();
        }
        for (int i = 0; i < count; i++) {
            $$(".number__button--add").first().click();
        }
        $(id("confirm-button")).click();
        sleep(777);
    }
    
    public static String extractUUID(String url) {
        Matcher matcher = UUID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
    
    //    @PostConstruct
    public void onStart() {
        options = new TreeMap<>(cacheService.getApolloKinoData());
        if (options.isEmpty()) {
            onSchedule();
            options = new TreeMap<>(cacheService.getApolloKinoData());
        }
    }
    
    //    @Scheduled(cron = "0 15 3,9,15,21 * * *")
    public void onSchedule() {
        init();
        cacheService.updateApolloKinoData(options);
    }
    
    public void init() {
        log.info("Initializing Apollo Kino");
        if (List.of(environment.getActiveProfiles()).contains(TEST_PROFILE)) {
            log.info("Skipping initialization in test profile");
            return;
        }
        long startTime = System.nanoTime();
        try {
            LocalDateTime today = LocalDateTime.now();
            List<String> acceptedDays = IntStream.range(0, 5)
                    .mapToObj(today::plusDays)
                    .map(d -> d.format(DATE_TIME_FORMATTER))
                    .toList();
            Cinema cinema = cinemaInRandomOrder();
            log.info("Initializing cinema {}", cinema);
            init(cinema, acceptedDays);
        } catch (Exception e) {
            log.error("Failed to init", e);
        } finally {
            Selenide.closeWebDriver();
            log.info("Init took {} seconds", TimeUtility.durationInSeconds(startTime).asString());
        }
    }
    
    private List<Cinema> cinemasInRandomOrder() {
        List<Cinema> cinemas = Arrays.asList(Cinema.values());
        Collections.shuffle(cinemas);
        return cinemas;
    }
    
    private Cinema cinemaInRandomOrder() {
        return cinemasInRandomOrder().getFirst();
    }
    
    private void init(Cinema cinema, List<String> acceptedDays) {
        options.computeIfAbsent(cinema, k -> new TreeMap<>());
        List<String> idsToRemove = options.get(cinema).keySet().stream()
                .map(optionList -> optionList.format(DATE_TIME_FORMATTER))
                .filter(day -> !acceptedDays.contains(day))
                .toList();
        idsToRemove.forEach(s -> options.get(cinema).remove(LocalDate.parse(s, DATE_TIME_FORMATTER)));
        open(cinema.getUrl());
        getWebDriver().manage().window().maximize();
        $(".cky-btn-accept").click();
        ElementsCollection elements = $$(".day-picker__choice");
        for (int i = 0; i < elements.size(); i++) {
            elements = $$(".day-picker__choice");
            SelenideElement dayChoice = elements.get(i);
            String selectedDayValue = dayChoice.find(tagName("input")).val();
            if (!acceptedDays.contains(selectedDayValue)) {
                continue;
            }
            log.info("Selected day {} for cinema {}", selectedDayValue, cinema);
            dayChoice.click();
            sleep(2000);
            TreeMap<String, String> movieTitles = new TreeMap<>();
            $$(".schedule-card__title")
                    .forEach(m -> movieTitles.put(m.text(), m.parent().parent().find(tagName("a")).getAttribute("href")));
            
            List<Option> movieOptions = new ArrayList<>();
            for (Map.Entry<String, String> element : movieTitles.entrySet()) {
                open(element.getValue());
                sleep(444);
                SelenideElement originalTitleElement = $(".movie-details__original-title");
                String movieOriginalTitle = "";
                if (originalTitleElement.exists()) {
                    String text = originalTitleElement.text();
                    asyncRunner.run(() -> movieDetailsService.fetchMovieDetails(text));
                    movieOriginalTitle = text;
                }
                $(".movie-details__button").click();
                sleep(444);
                List<ScreenTime> screenTimes = new ArrayList<>();
                ElementsCollection screeningElements = $$(className("screening-card__top"));
                for (SelenideElement screeningElement : screeningElements) {
                    String hallName = screeningElement.find(className("screening-card__hall")).text();
                    String time = screeningElement.find(className("screening-card__time")).text();
                    if (screenConfig.isValidHall(cinema, hallName)) {
                        ScreenTime screenTime = new ScreenTime(
                                LocalTime.parse(time),
                                screeningElement.parent().find(tagName("a")).getAttribute("href"),
                                hallName
                        );
                        screenTimes.add(screenTime);
                    }
                }
                Option movieOption = Option.builder()
                        .movie(element.getKey())
                        .screenTimes(screenTimes)
                        .movieOriginalTitle(movieOriginalTitle)
                        .build();
                if (screenTimes.isEmpty()) {
                    continue;
                }
                movieOptions.add(movieOption);
            }
            LocalDate chosenDate = Optional.ofNullable(selectedDayValue)
                    .map(d -> LocalDate.parse(d, DATE_TIME_FORMATTER))
                    .orElseThrow(() -> new RuntimeException("Date not found"));
            this.options.get(cinema).put(chosenDate, movieOptions);
            open(cinema.getUrl());
        }
        for (List<Option> optionList : options.get(cinema).values()) {
            for (Option option : optionList) {
                movieDetailsService.fetchMovieDetails(option.getMovieOriginalTitle())
                        .ifPresent(d -> {
                            try {
                                option.setImdbRating(Double.parseDouble(d.getImdbRating()));
                            } catch (Exception e) {
                                option.setImdbRating(0.0);
                                log.error("Failed to parse rating", e);
                            }
                        });
            }
        }
        Selenide.clearBrowserCookies();
    }
    
    public Optional<ScreenTime> screenTime(ApolloKinoSession session) {
        return this.options.get(session.getCinema()).get(session.getSelectedDate()).stream()
                .filter(screen -> screen.getMovie().equals(session.getSelectedMovie()))
                .map(Option::getScreenTimes)
                .flatMap(List::stream)
                .filter(t -> t.getTime().equals(session.getSelectedTime()))
                .findFirst();
    }
    
    public Map<LocalDate, List<Option>> getOptions(Cinema cinema) {
        AtomicBoolean updatedImdbRating = new AtomicBoolean(false);
        LocalDateTime currentDateTime = LocalDateTime.now();
        Map<LocalDate, List<Option>> filteredOptions = new TreeMap<>();
        for (Map.Entry<LocalDate, List<Option>> entry : options.get(cinema).entrySet()) {
            LocalDate key = entry.getKey();
            List<Option> movieOptions = entry.getValue();
            List<Option> newMovieOptions = new ArrayList<>();
            for (Option movieOption : movieOptions) {
                List<ScreenTime> screenTimes = movieOption.getScreenTimes();
                if (movieOption.getUpdatedAt().isBefore(currentDateTime.toInstant(ZoneOffset.UTC).minus(7, ChronoUnit.DAYS))) {
                    asyncRunner.run(() -> movieDetailsService.fetchMovieDetails(movieOption.getMovieOriginalTitle())
                            .map(MovieDetails::getImdbRating)
                            .map(Double::parseDouble)
                            .ifPresent(movieOption::setImdbRating));
                    updatedImdbRating.set(true);
                    movieOption.setUpdatedAt(currentDateTime.toInstant(ZoneOffset.UTC));
                }
                List<ScreenTime> newScreenTimes = new ArrayList<>();
                for (ScreenTime screenTime : screenTimes) {
                    LocalDateTime dateTime = LocalDateTime.of(key, screenTime.getTime());
                    if (currentDateTime.isBefore(dateTime)) {
                        newScreenTimes.add(screenTime);
                    }
                }
                if (!newScreenTimes.isEmpty()) {
                    Option newMovieOption = Option.builder()
                            .movie(movieOption.getMovie())
                            .screenTimes(newScreenTimes)
                            .movieOriginalTitle(movieOption.getMovieOriginalTitle())
                            .imdbRating(movieOption.getNumericalImdbRating())
                            .build();
                    newMovieOptions.add(newMovieOption);
                }
            }
            if (!newMovieOptions.isEmpty()) {
                filteredOptions.put(key, newMovieOptions);
            }
        }
        if (updatedImdbRating.get()) {
            log.info("Updated IMDB rating. Updating cache");
            cacheService.updateApolloKinoData(options);
        }
        return filteredOptions;
    }
    
    private static int toSeconds(String time) {
        try {
            String[] split = time.split(":");
            int minutes = Integer.parseInt(split[0]) * 60;
            int seconds = Integer.parseInt(split[1]);
            return minutes + seconds;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse time", e);
        }
    }
    
    public Optional<Map.Entry<File, Set<StarSeat>>> book(ApolloKinoSession session) {
        Optional<ScreenTime> screenTime = screenTime(session);
        if (screenTime.isEmpty()) {
            log.error("Screen time not found");
            return Optional.empty();
        }
        String rowAndSeat = session.getRowAndSeat();
        try {
            open(screenTime.get().getUrl());
            getWebDriver().manage().window().maximize();
            ElementsCollection elements = $$(className("user__account-name"));
            boolean acceptedTerms = false;
            if (!elements.isEmpty()) {
                acceptedTerms = elements.texts().stream().anyMatch("Konrad"::equals);
            }
            if (!acceptedTerms) {
                $(".cky-btn-accept").click();
                login();
            }
            int selectedSeatsCount = Math.max(session.getSelectedStarSeats().size(), session.getSeatCount());
            selectSeats(selectedSeatsCount);
            String currentUrl = getWebDriver().getCurrentUrl();
            String uuid = extractUUID(currentUrl);
            sleep(333);
            String saal = $(".schedule-card__title-container").findAll(tagName("p")).last().text();
            Screen screen = screenConfig.getScreen(session.getCinema(), saal);
            int[] coords = coordinates(screen, rowAndSeat);
            int x = coords[0];
            int y = coords[1];
            String showId = extractShowId(currentUrl);
            String seatPlanImageUrl = "https://www.apollokino.ee/websales/seating/" + uuid + "/seatplanimage/" + showId +
                    "/" + screen.id() + "?posX=" + x + "&posY=" + y + "&posImgWidth=798&t=" + Instant.now().toEpochMilli();
            open(seatPlanImageUrl);
            sleep(444);
            open(currentUrl);
            sleep(444);
            ElementsCollection iframeElements = $$(tagName("iframe"));
            switchTo().frame(iframeElements.get(0));
            File screenshot = $(id("seat-plan-img")).screenshot();
            switchTo().defaultContent();
            $$(tagName("button")).find(text("Maksma")).click();
            sleep(333);
            
            ElementsCollection starSeatRows = $$(className("checkout-card__title")).filter(text(session.getSelectedMovie()))
                    .first()
                    .parent()
                    .parent()
                    .parent()
                    .findAll(tagName("tr"))
                    .filter(text("Staaritoolid"));
            Set<StarSeat> starSeats = new HashSet<>();
            for (SelenideElement starSeatRow : starSeatRows) {
                List<String> texts = starSeatRow.findAll(".table-list__item").texts();
                starSeats.add(StarSeat.builder()
                        .row(texts.get(0))
                        .seat(texts.get(1))
                        .build());
            }
            return Optional.of(new SimpleEntry<>(screenshot, starSeats));
        } catch (Exception e) {
            log.error("Failed to book", e);
            return Optional.empty();
        } finally {
            Selenide.closeWebDriver();
        }
    }
    
    public Optional<Map.Entry<File, Set<StarSeat>>> reBook(ApolloKinoSession session) {
        Optional<ScreenTime> screenTime = screenTime(session);
        if (screenTime.isEmpty()) {
            log.error("Screen time not found");
            return Optional.empty();
        }
        try {
            open("https://www.apollokino.ee/account/tickets");
            getWebDriver().manage().window().maximize();
            $(".cky-btn-accept").click();
            login();
            
            SelenideElement headerTimer = $(".header__timer");
            if (!headerTimer.exists()) {
                return book(session);
            }
            headerTimer.click();
            int seconds = toSeconds($(".cart-session-timer").text()) - 7;
            CountdownTimer.startTimer(seconds);
            $$(By.tagName("button")).find(text("Eemalda piletid")).click();
            return book(session);
        } catch (Exception e) {
            log.error("Failed to re-book", e);
            return Optional.empty();
        } finally {
            Selenide.closeWebDriver();
        }
        
    }
    
    private void login() {
        $(name("username")).setValue(username);
        $(name("password")).setValue(password);
        $(".user__login-submit").click();
    }
    
    private String extractShowId(String currentUrl) {
        Pattern pattern = Pattern.compile("show%2F(\\d+)$");
        Matcher matcher = pattern.matcher(currentUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new RuntimeException("Show id not found");
    }
    
    private int extractInt(String val) {
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return 0;
        }
    }
    
    public int[] coordinates(Screen screen, String rowAndSeat) {
        String[] split = rowAndSeat.split("K");
        String row = split[0];
        int[] points = screen.coordinates().get(row);
        int currentSeat = extractInt(split[1]);
        int index = (currentSeat - 1) / 2;
        int x = (currentSeat % 2 == 1 ? points[0] : points[1]) + points[2] * index;
        return new int[]{x, points[3]};
    }
    
}
