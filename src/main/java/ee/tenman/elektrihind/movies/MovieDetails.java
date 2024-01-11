package ee.tenman.elektrihind.movies;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieDetails implements Serializable {
    private static final long serialVersionUID = 14454325423L;

    private String imdbID;
    private String imdbRating;

    public String getImdbLink() {
        return "https://www.imdb.com/title/" + imdbID;
    }
}
