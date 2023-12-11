package ee.tenman.elektrihind.car.vision;

import ee.tenman.elektrihind.car.vision.GoogleVisionApiResponse.EntityAnnotation;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ee.tenman.elektrihind.car.vision.GoogleVisionApiRequest.FeatureType.LABEL_DETECTION;
import static ee.tenman.elektrihind.car.vision.GoogleVisionApiRequest.FeatureType.TEXT_DETECTION;

@Service
public class GoogleVisionService {

    private static final String REGEX = "\\b\\d{3}\\s?[A-Za-z]{3}\\b";
    private static final Pattern PATTERN = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);
    public static final String VEHICLE_REGISTRATION_PLATE = "Vehicle registration plate";

    @Resource
    private GoogleVisionClient googleVisionClient;

    public Optional<String> getPlateNumber(byte[] imageBytes) {
        String plateNr = null;
        GoogleVisionApiRequest googleVisionApiRequest = new GoogleVisionApiRequest(imageBytes, LABEL_DETECTION);

        GoogleVisionApiResponse googleVisionApiResponse = googleVisionClient.analyzeImage(googleVisionApiRequest);

        boolean hasVehicleRegistrationPlate = googleVisionApiResponse.getLabelAnnotations().stream()
                .anyMatch(labelAnnotation -> VEHICLE_REGISTRATION_PLATE.equalsIgnoreCase(labelAnnotation.getDescription()));

        if (!hasVehicleRegistrationPlate) {
            return Optional.empty();
        }

        googleVisionApiRequest = new GoogleVisionApiRequest(imageBytes, TEXT_DETECTION);
        googleVisionApiResponse = googleVisionClient.analyzeImage(googleVisionApiRequest);

        for (EntityAnnotation textAnnotation : googleVisionApiResponse.getTextAnnotations()) {
            String description = textAnnotation.getDescription();
            Matcher matcher = PATTERN.matcher(description);
            if (matcher.find()) {
                plateNr = matcher.group().replace(" ", "").toUpperCase();
            }
        }

        return Optional.ofNullable(plateNr);
    }
}

