package ee.tenman.elektrihind.car.vision;

import ee.tenman.elektrihind.car.vision.GoogleVisionApiResponse.EntityAnnotation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Optional;
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

    @Resource
    private GoogleVisionClient googleVisionClient;

    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1500))
    public Optional<String> getPlateNumber(byte[] imageBytes) {
        String plateNr = null;
        GoogleVisionApiRequest googleVisionApiRequest = new GoogleVisionApiRequest(imageBytes, LABEL_DETECTION);

        GoogleVisionApiResponse googleVisionApiResponse = googleVisionClient.analyzeImage(googleVisionApiRequest);

        boolean hasVehicleRegistrationPlateNumber = googleVisionApiResponse.getLabelAnnotations().stream()
                .anyMatch(labelAnnotation -> VEHICLE_REGISTRATION_PLATE.equalsIgnoreCase(labelAnnotation.getDescription()));

        if (!hasVehicleRegistrationPlateNumber) {
            return Optional.empty();
        }

        googleVisionApiRequest = new GoogleVisionApiRequest(imageBytes, TEXT_DETECTION);
        googleVisionApiResponse = googleVisionClient.analyzeImage(googleVisionApiRequest);

        for (EntityAnnotation textAnnotation : googleVisionApiResponse.getTextAnnotations()) {
            String description = textAnnotation.getDescription();
            Matcher matcher = CAR_PLATE_NUMBER_PATTERN.matcher(description);
            if (matcher.find()) {
                plateNr = matcher.group().replace(" ", "").toUpperCase();
                return Optional.of(plateNr);
            }
        }

        return Optional.empty();
    }
}

