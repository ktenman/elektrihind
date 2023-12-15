package ee.tenman.elektrihind.car.googlevision;

import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static ee.tenman.elektrihind.car.googlevision.GoogleVisionApiRequest.FeatureType.LABEL_DETECTION;
import static ee.tenman.elektrihind.car.googlevision.GoogleVisionApiRequest.FeatureType.TEXT_DETECTION;

@Data
public class GoogleVisionApiRequest {
    public GoogleVisionApiRequest(String base64EncodedImage, FeatureType... featureTypes) {
        // Set the content of the Image object
        Image image = new Image();
        image.setContent(base64EncodedImage);

        if (featureTypes.length == 0) {
            featureTypes = new FeatureType[]{LABEL_DETECTION, TEXT_DETECTION};
        }

        List<Feature> features = Stream.of(featureTypes).map(featureType -> {
            Feature feature = new Feature();
            feature.setType(featureType.name());
            return feature;
        }).toList();

        // Create the request object
        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setImage(image);
        annotateImageRequest.setFeatures(features);

        // Set the requests list with the created request object
        this.requests = Collections.singletonList(annotateImageRequest);
    }

    private List<AnnotateImageRequest> requests;

    public enum FeatureType {
        LABEL_DETECTION,
        TEXT_DETECTION;
    }

    @Data
    public static class AnnotateImageRequest {
        private Image image;
        private List<Feature> features;
    }

    @Data
    public static class Image {
        private String content; // Base64-encoded image data
    }

    @Data
    public static class Feature {
        private String type; // Type of detection to perform
    }
}
