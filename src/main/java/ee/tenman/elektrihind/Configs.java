package ee.tenman.elektrihind;

import java.time.format.DateTimeFormatter;

interface Configs {
    String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm";
    DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
}
