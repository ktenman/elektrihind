package ee.tenman.elektrihind.apollo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Option {

    private String movie;
    private String movieOriginalTitle;
    private List<ScreenTime> screenTimes = new ArrayList<>();
    private Double imdbRating;

    @JsonIgnore
    public String getMovieTitleWithImdbRating() {
        return imdbRating == null || imdbRating.equals(0.0) ? movie : movie + " [" + imdbRating + "]";
    }

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
