package ee.tenman.elektrihind.car.googlevision;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@Slf4j
public class GoogleVisionService {

    private static final String REGEX = "\\b\\d{3}\\s?[A-Z]{3}\\b";
    public static final Pattern CAR_PLATE_NUMBER_PATTERN = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);
    private static final String VEHICLE_REGISTRATION_PLATE = "Vehicle registration plate";
    private static final Set<String> VEHICLE_LABELS = Set.of("vehicle", "car");
    @Resource
    private GoogleVisionClient googleVisionClient;

    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1500))
    public Map<String, String> getPlateNumber(String base64EncodedImage, UUID uuid) {
        MDC.put("uuid", uuid.toString());
        log.debug("Starting plate number detection from image. Image size: {} bytes", base64EncodedImage.getBytes().length);
        try {
            log.debug("Encoded image to base64");

            GoogleVisionApiRequest googleVisionApiRequest = new GoogleVisionApiRequest(base64EncodedImage, GoogleVisionApiRequest.FeatureType.LABEL_DETECTION);
            GoogleVisionApiResponse googleVisionApiResponse = googleVisionClient.analyzeImage(googleVisionApiRequest);
            log.info("Received label detection response: {}", googleVisionApiResponse);

            boolean hasVehicleOrCar = googleVisionApiResponse.getLabelAnnotations().stream()
                    .anyMatch(labelAnnotation -> Stream.of(labelAnnotation.getDescription().toLowerCase().split("\\s+"))
                            .anyMatch(VEHICLE_LABELS::contains));
            log.debug("Vehicle/car detected: {}", hasVehicleOrCar);

            Map<String, String> response = new HashMap<>();
            String hasCar = hasCar(googleVisionApiResponse.getLabelAnnotations());
            response.put("hasCar", hasCar);
            if (!hasVehicleOrCar) {
                return response;
            }

            googleVisionApiRequest = new GoogleVisionApiRequest(base64EncodedImage, GoogleVisionApiRequest.FeatureType.TEXT_DETECTION);
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
        } finally {
            MDC.remove("uuid");
        }
    }

    private String hasCar(List<GoogleVisionApiResponse.EntityAnnotation> labelAnnotations) {
        for (GoogleVisionApiResponse.EntityAnnotation labelAnnotation : labelAnnotations) {
            if (labelAnnotation.getDescription().contains("car")) {
                return Boolean.TRUE.toString();
            }
            if (labelAnnotation.getDescription().contains("vehicle")) {
                return Boolean.TRUE.toString();
            }
        }
        return Boolean.FALSE.toString();
    }
}

