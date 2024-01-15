package ee.tenman.elektrihind.apollo;

import java.time.LocalTime;

public record ScreenTime(
        LocalTime time,
        String url,
        String hall
) {

}
