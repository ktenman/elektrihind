package ee.tenman.elektrihind.car.vision;

import lombok.Data;

import java.util.List;

@Data
public class GoogleVisionApiResponse {
    private List<EntityAnnotation> labelAnnotations;
    private List<EntityAnnotation> textAnnotations;

    public void setResponses(List<AnnotateImageResponse> responses) {
        if (responses != null && responses.size() == 1) {
            AnnotateImageResponse annotateImageResponse = responses.getFirst();
            this.labelAnnotations = annotateImageResponse.getLabelAnnotations();
            this.textAnnotations = annotateImageResponse.getTextAnnotations();
        } else {
            // Handle the case where there isn't exactly one item in the list
            throw new IllegalArgumentException("Expected exactly one response");
        }
    }

    @Data
    public static class AnnotateImageResponse {
        private List<EntityAnnotation> labelAnnotations;
        private List<EntityAnnotation> textAnnotations;
    }

    @Data
    public static class EntityAnnotation {
        private String description;
        private Float score;
    }
}
