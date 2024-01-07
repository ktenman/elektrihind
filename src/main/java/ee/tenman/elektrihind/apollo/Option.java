package ee.tenman.elektrihind.apollo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Option {
    private String movie;
    private List<ScreenTime> screenTimes;

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class ScreenTime {
        private LocalTime time;
        private String url;
        private String hall;
    }
}
