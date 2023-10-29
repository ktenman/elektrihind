package ee.tenman.elektrihind.movies;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieDetails {
    private String imdbID;
    private String imdbRating;

    public String getImdbLink() {
        return "https://www.imdb.com/title/" + imdbID;
    }
}
