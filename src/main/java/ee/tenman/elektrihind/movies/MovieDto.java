package ee.tenman.elektrihind.movies;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieDto {
    private String title;
    private String director;
    private String country;
    private String year;
    private double imdbRating;
    private String imdbLink;
    private String duration;
    private String language;
    private String poffUrl;

    public String getImdbId() {
        return imdbLink.substring(imdbLink.lastIndexOf("/") + 1);
    }
}
