package ee.tenman.elektrihind.movies.imdb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IMDB {
    private String id;
    private String review_api_path;
    private String imdb;
    private String contentType;
    private String productionStatus;
    private String title;
    private String image;
    private List<String> images;
    private String plot;
    private Rating rating;
    private Award award;
    private String contentRating;
    private List<String> genre;
    private ReleaseDetailed releaseDetailed;
    private Integer year;
    private List<SpokenLanguages> spokenLanguages;
    private List<String> filmingLocations;
    private String runtime;
    private Integer runtimeSeconds;
    private List<String> actors;
    private List<String> directors;
    private List<TopCredits> top_credits;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rating {
        private Integer count;
        private Double star;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Award {
        private Integer wins;
        private Integer nominations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReleaseDetailed {
        private Integer day;
        private Integer month;
        private Integer year;
        private ReleaseLocation releaseLocation;
        private List<OriginLocations> originLocations;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ReleaseLocation {
            private String country;
            private String cca2;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OriginLocations {
            private String country;
            private String cca2;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpokenLanguages {
        private String language;
        private String id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCredits {
        private String name;
        private List<String> value;
    }
}
