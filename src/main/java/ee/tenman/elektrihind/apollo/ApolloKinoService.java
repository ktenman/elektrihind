package ee.tenman.elektrihind.apollo;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import ee.tenman.elektrihind.config.ScreenConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.sleep;
import static com.codeborne.selenide.Selenide.switchTo;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.name;
import static org.openqa.selenium.By.tagName;

@Service
@Slf4j
public class ApolloKinoService {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String FIRST_URL = "https://www.apollokino.ee/schedule?theatreAreaID=1017";
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    @Getter
    private final Map<LocalDate, List<Option>> options = new LinkedHashMap<>();
    @Value("${applo-kino.username}")
    private String username;
    @Value("${applo-kino.password}")
    private String password;
    @Resource
    private ScreenConfiguration screenConfig;

    private static void selectOneSeat() {
        $$(".radio-card__text").find(text("Staaritoolid")).click();
        sleep(2000);
        for (int i = 0; i < 10; i++) {
            SelenideElement subtractButton = $$(".number__button--subtract ").first();
            if (subtractButton.has(attribute("disabled"))) {
                break;
            }
            subtractButton.click();
        }
        $$(".number__button--add").first().click();
        $(id("confirm-button")).click();
        sleep(2000);
    }

    public static String extractUUID(String url) {
        Matcher matcher = UUID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    @PostConstruct
    public void onStart() {
        init();
    }

    @Scheduled(cron = "1 0 0 * * *")
    public void onSchedule() {
        init();
    }

    public void init() {
        open(FIRST_URL);

        getWebDriver().manage().window().maximize();
        $(".cky-btn-accept").click();
        for (int i = 0; i < 3; i++) {
            ElementsCollection elements = $$(".day-picker__choice");
            SelenideElement dayChoice = elements.get(i);
            dayChoice.click();
            sleep(2000);
            TreeMap<String, String> movieTitles = new TreeMap<>();
            $$(".schedule-card__title")
                    .forEach(m -> movieTitles.put(m.text(), m.parent().parent().find(tagName("a")).getAttribute("href")));

            List<Option> movieOptions = new ArrayList<>();
            for (Map.Entry<String, String> element : movieTitles.entrySet()) {
                open(element.getValue());
                sleep(444);
                $(".movie-details__button").click();
                sleep(444);
                List<Option.ScreenTime> screenTimes = new ArrayList<>();
                ElementsCollection screeningElements = $$(className("screening-card__top"));
                for (SelenideElement screeningElement : screeningElements) {
                    String hallName = screeningElement.find(className("screening-card__hall")).text();
                    String time = screeningElement.find(className("screening-card__time")).text();
                    if (screenConfig.isValidHall(hallName)) {
                        Option.ScreenTime screenTime = Option.ScreenTime.builder()
                                .time(LocalTime.parse(time))
                                .url(screeningElement.parent().find(tagName("a")).getAttribute("href"))
                                .hall(hallName)
                                .build();
                        screenTimes.add(screenTime);
                    }
                }
                Option movieOption = Option.builder()
                        .movie(element.getKey())
                        .screenTimes(screenTimes)
                        .build();
                if (screenTimes.isEmpty()) {
                    continue;
                }
                movieOptions.add(movieOption);
            }
            String selectedDay = dayChoice.find(tagName("input")).val();
            LocalDate chosenDate = LocalDate.parse(selectedDay, DATE_TIME_FORMATTER);
            this.options.put(chosenDate, movieOptions);
            open(FIRST_URL);
        }
        Selenide.closeWindow();
    }

    public Option.ScreenTime screenTime(ApolloKinoSession session) {
        return this.getOptions().get(session.getSelectedDate()).stream()
                .filter(screen -> screen.getMovie().equals(session.getSelectedMovie()))
                .map(Option::getScreenTimes)
                .flatMap(List::stream)
                .filter(t -> t.getTime().equals(session.getSelectedTime()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Screen time not found for "
                        + session.getSelectedDate() + " " + session.getSelectedMovie() + " " + session.getSelectedTime()));
    }

    public Optional<File> book(ApolloKinoSession session) {
//        Configuration.headless = false;

        Option.ScreenTime screenTime = screenTime(session);
        String koht = session.getKoht();

        try {
            open(screenTime.getUrl());

            getWebDriver().manage().window().maximize();
            $(".cky-btn-accept").click();

            login();
//            String currentUrl1 = getWebDriver().getCurrentUrl();
            selectOneSeat();

//            tühista();
//            open(currentUrl1);
//            selectOneSeat();

            String currentUrl = getWebDriver().getCurrentUrl();
            String uuid = extractUUID(currentUrl);

            sleep(500);
            String saal = $(".schedule-card__title-container").findAll(tagName("p")).last().text();

            Screen screen = screenConfig.getScreen(saal);

            boolean validSeat = screen.isValidSeat(koht);
            if (!validSeat) {
                throw new RuntimeException("Invalid seat");
            }

            int[] coords = coordinates(screen, koht);
            int x = coords[0];
            int y = coords[1];

            String showId = extractShowId(currentUrl);

            String seatPlanImageUrl = "https://www.apollokino.ee/websales/seating/" + uuid + "/seatplanimage/" + showId +
                    "/" + screen.getId() + "?posX=" + x + "&posY=" + y + "&posImgWidth=798&t=" + Instant.now().toEpochMilli();

            open(seatPlanImageUrl);

            sleep(777);

            open(currentUrl);

            sleep(777);

            ElementsCollection iframeElements = $$(tagName("iframe"));

            switchTo().frame(iframeElements.get(0));

            File screenshot = $(id("seat-plan-img")).screenshot();

            switchTo().defaultContent();

            $$(tagName("button")).find(text("Maksma")).click();

            return Optional.ofNullable(screenshot);
        } catch (Exception e) {
            log.error("Failed to book", e);
            return Optional.empty();
        } finally {
            Selenide.closeWindow();
        }

    }

    private void login() {
        $(name("username")).setValue(username);
        $(name("password")).setValue(password);
        $(".user__login-submit").click();
    }

    private void tühista() {
        $$(".button--block-xs").find(text("Tühista")).click();
        $$(".modal__button").find(text("TÜHISTA")).click();
        sleep(1000);
        Selenide.refresh();
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

    public int[] coordinates(Screen screen, String koht) {
        String[] split = koht.split("K");
        String row = split[0];
        int[] points = screen.getCoordinates().get(row);
        int currentSeat = extractInt(split[1]);
        int index = (currentSeat - 1) / 2;
        int x = (currentSeat % 2 == 1 ? points[0] : points[1]) + points[2] * index;
        return new int[]{x, points[3]};
    }
}
