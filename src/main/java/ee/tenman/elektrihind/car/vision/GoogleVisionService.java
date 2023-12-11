package ee.tenman.elektrihind.car.vision;

import ee.tenman.elektrihind.car.vision.GoogleVisionApiResponse.EntityAnnotation;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GoogleVisionService {

    private static final String REGEX = "\\b\\d{3}\\s?[A-Za-z]{3}\\b";
    private static final Pattern PATTERN = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);

    @Resource
    private GoogleVisionClient googleVisionClient;

    public Optional<String> getPlateNumber(byte[] imageBytes) {
        String plateNr = null;
        GoogleVisionApiRequest requestBody = new GoogleVisionApiRequest(imageBytes);

        GoogleVisionApiResponse googleVisionApiResponse = googleVisionClient.analyzeImage(requestBody);

        boolean hasVehicleRegistrationPlate = googleVisionApiResponse.getLabelAnnotations().stream()
                .anyMatch(labelAnnotation -> labelAnnotation.getDescription().equalsIgnoreCase("Vehicle registration plate"));

        if (!hasVehicleRegistrationPlate) {
            return Optional.empty();
        }

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

