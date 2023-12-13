package ee.tenman.elektrihind.car.vision;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ee.tenman.elektrihind.car.vision.GoogleVisionApiRequest.FeatureType.LABEL_DETECTION;
import static ee.tenman.elektrihind.car.vision.GoogleVisionApiRequest.FeatureType.TEXT_DETECTION;

@Service
@Slf4j
public class GoogleVisionService {

    private static final String REGEX = "\\b\\d{3}\\s?[A-Z]{3}\\b";
    public static final Pattern CAR_PLATE_NUMBER_PATTERN = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);
    private static final String VEHICLE_REGISTRATION_PLATE = "Vehicle registration plate";
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

    @Resource
    private GoogleVisionClient googleVisionClient;

    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1500))
    public Map<String, Object> getPlateNumber(String base64EncodedImage) {
        log.debug("Starting plate number detection from image. Image size: {} bytes", base64EncodedImage.getBytes().length);
        try {
            log.debug("Encoded image to base64");

            GoogleVisionApiRequest googleVisionApiRequest = new GoogleVisionApiRequest(base64EncodedImage, LABEL_DETECTION);
            GoogleVisionApiResponse googleVisionApiResponse = googleVisionClient.analyzeImage(googleVisionApiRequest);
            log.info("Received label detection response: {}", googleVisionApiResponse);

            boolean hasVehicleRegistrationPlateNumber = googleVisionApiResponse.getLabelAnnotations().stream()
                    .anyMatch(labelAnnotation -> VEHICLE_REGISTRATION_PLATE.equalsIgnoreCase(labelAnnotation.getDescription()));
            log.debug("Vehicle registration plate detected: {}", hasVehicleRegistrationPlateNumber);

            Map<String, Object> response = new HashMap<>();
            Boolean hasCar = hasCar(googleVisionApiResponse.getLabelAnnotations());
            response.put("hasCar", hasCar);
            if (!hasVehicleRegistrationPlateNumber) {
                return response;
            }

            googleVisionApiRequest = new GoogleVisionApiRequest(base64EncodedImage, TEXT_DETECTION);
            googleVisionApiResponse = googleVisionClient.analyzeImage(googleVisionApiRequest);
            String[] strings = googleVisionApiResponse.getTextAnnotations().stream().findFirst().map(s -> StringUtils.split(s.getDescription(), "\n")).orElse(new String[0]);
            log.info("Received text detection response: {}", googleVisionApiResponse);

            for (String description : strings) {
                log.debug("Processing text annotation: {}", description);
                Matcher matcher = CAR_PLATE_NUMBER_PATTERN.matcher(description);
                if (matcher.find()) {
                    String plateNr = matcher.group().replace(" ", "").toUpperCase();
                    log.debug("Plate number found: {}", plateNr);
                    response.put("plateNumber", plateNr);
                    return response;
                }
            }
            return response;
        } catch (Exception e) {
            log.error("Error during plate number detection", e);
            return Map.of();
        }
    }

    private Boolean hasCar(List<GoogleVisionApiResponse.EntityAnnotation> labelAnnotations) {
        for (GoogleVisionApiResponse.EntityAnnotation labelAnnotation : labelAnnotations) {
            if (labelAnnotation.getDescription().contains("car")) {
                return Boolean.TRUE;
            }
            if (labelAnnotation.getDescription().contains("vehicle")) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }
}

