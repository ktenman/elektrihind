package ee.tenman.elektrihind.car.vision;

import lombok.Data;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Data
public class GoogleVisionApiRequest {
    private List<AnnotateImageRequest> requests;

    public GoogleVisionApiRequest(byte[] imageBytes) {
        // Convert the image bytes to a base64 encoded string
        String base64EncodedImage = Base64.getEncoder().encodeToString(imageBytes);

        // Set the content of the Image object
        Image image = new Image();
        image.setContent(base64EncodedImage);

        // Create label detection feature
        Feature labelDetectionFeature = new Feature();
        labelDetectionFeature.setType("LABEL_DETECTION");

        // Create text detection feature
        Feature textDetectionFeature = new Feature();
        textDetectionFeature.setType("TEXT_DETECTION");

        // Create the request object
        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setImage(image);
        annotateImageRequest.setFeatures(Arrays.asList(labelDetectionFeature, textDetectionFeature));

        // Set the requests list with the created request object
        this.requests = Collections.singletonList(annotateImageRequest);
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
