package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.car.googlevision.GoogleVisionService;
import ee.tenman.elektrihind.car.openai.OpenAiVisionService;
import ee.tenman.elektrihind.queue.QueueTextDetectionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static ee.tenman.elektrihind.utility.TimeUtility.durationInSeconds;

@Service
@Slf4j
public class PlateDetectionService {

    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
    private static final MessageDigest MESSAGE_DIGEST;

    static {
        try {
            MESSAGE_DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Resource
    private GoogleVisionService googleVisionService;

    @Resource
    private OpenAiVisionService openAiVisionService;

    @Resource
    QueueTextDetectionService queueTextDetectionService;

    public String buildMD5(String input) {
        byte[] hashInBytes = MESSAGE_DIGEST.digest(input.getBytes(StandardCharsets.UTF_8));

        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : hashInBytes) {
            stringBuilder.append(String.format("%02x", b));
        }
        return stringBuilder.toString();
    }

    public Optional<String> detectPlate(byte[] image) {
        long startTime = System.nanoTime();
        UUID uuid = UUID.randomUUID();
        MDC.put("uuid", uuid.toString());
        String base64EncodedImage = BASE64_ENCODER.encodeToString(image);
        String encodedImageMD5 = buildMD5(base64EncodedImage);
        log.debug("Starting plate detection. Image size: {} bytes", base64EncodedImage.getBytes().length);

        try {
            Optional<String> plateNumber = queueTextDetectionService.detectPlate(base64EncodedImage, uuid);
            if (plateNumber.isPresent()) {
                log.info("Plate detected via queue: {}", plateNumber.get());
                return plateNumber;
            }

            Map<String, String> googleVisionResponse = googleVisionService.getPlateNumber(base64EncodedImage, uuid, encodedImageMD5);
            plateNumber = Optional.ofNullable(googleVisionResponse.get("plateNumber"));
            if (plateNumber.isPresent()) {
                log.info("Plate detected by GoogleVisionService: {}", plateNumber.get());
                return plateNumber;
            }

            boolean hasCar = Optional.ofNullable(googleVisionResponse.get("hasCar"))
                    .map(Boolean::valueOf)
                    .orElse(false);
            if (!hasCar) {
                log.debug("GoogleVisionService did not detect the car");
                return Optional.empty();
            }

            plateNumber = openAiVisionService.getPlateNumber(base64EncodedImage, uuid, encodedImageMD5);
            if (plateNumber.isPresent()) {
                log.info("Plate detected by OpenAiVisionService: {}", plateNumber.get());
                return plateNumber;
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Error during plate detection", e);
            return Optional.empty();
        } finally {
            log.debug("Plate detection process completed in {} seconds", durationInSeconds(startTime));
            MDC.remove("uuid");
        }
    }

}
