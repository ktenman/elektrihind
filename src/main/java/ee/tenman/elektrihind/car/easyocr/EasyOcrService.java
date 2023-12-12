package ee.tenman.elektrihind.car.easyocr;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;

import static ee.tenman.elektrihind.car.vision.GoogleVisionService.CAR_PLATE_NUMBER_PATTERN;

@Service
@Slf4j
public class EasyOcrService {

    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

    @Resource
    private EasyOcrClient easyOcrClient;

    public Optional<String> imageToText(byte[] imageBytes) {
        log.debug("Starting image to text conversion");
        try {
            String base64Image = BASE64_ENCODER.encodeToString(imageBytes);
            log.debug("Encoded image to base64. Image size: {} bytes", imageBytes.length);
            EasyOcrRequest request = new EasyOcrRequest(base64Image);
            EasyOcrResponse response = easyOcrClient.decode(request);
            log.info("Decoded successfully. Response: {}", response);
            return Optional.ofNullable(response.getDecodedText());
        } catch (Exception e) {
            log.error("Failed to decode image", e);
            return Optional.empty();
        } finally {
            log.debug("Image to text conversion completed");
        }
    }

    public Optional<String> getPlateNumber(byte[] imageBytes) {
        log.debug("Starting plate number extraction from image");
        try {
            Optional<String> decode = imageToText(imageBytes);
            if (decode.isEmpty()) {
                log.debug("No text decoded from image");
                return Optional.empty();
            }
            Matcher matcher = CAR_PLATE_NUMBER_PATTERN.matcher(decode.get());
            if (matcher.find()) {
                String plateNumber = matcher.group().replace(" ", "").toUpperCase();
                log.debug("Plate number found: {}", plateNumber);
                return Optional.of(plateNumber);
            }
            log.debug("No plate number matched in the decoded text");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to extract plate number from image", e);
            return Optional.empty();
        } finally {
            log.debug("Plate number extraction completed");
        }
    }

}
