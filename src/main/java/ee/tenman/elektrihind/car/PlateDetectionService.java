package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.car.openai.OpenAiVisionService;
import ee.tenman.elektrihind.car.vision.GoogleVisionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static ee.tenman.elektrihind.utility.TimeUtility.durationInSeconds;

@Service
@Slf4j
public class PlateDetectionService {

    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

    @Resource
    private GoogleVisionService googleVisionService;

    @Resource
    private OpenAiVisionService openAiVisionService;

    @Resource
    QueuePlateDetectionService queuePlateDetectionService;

    public Optional<String> detectPlate(byte[] image) {
        long startTime = System.nanoTime();
        UUID uuid = UUID.randomUUID();
        MDC.put("uuid", uuid.toString());
        String base64EncodedImage = BASE64_ENCODER.encodeToString(image);
        log.debug("Starting plate detection [UUID: {}]. Image size: {} bytes", uuid, base64EncodedImage.getBytes().length);

        try {
            Optional<String> plateNumber = queuePlateDetectionService.detectPlate(base64EncodedImage, uuid);
            if (plateNumber.isPresent()) {
                log.info("Plate detected via queue [UUID: {}]: {}", uuid, plateNumber.get());
                return plateNumber;
            }

            Map<String, Object> googleVisionResponse = googleVisionService.getPlateNumber(base64EncodedImage);
            plateNumber = Optional.ofNullable((String) googleVisionResponse.get("plateNumber"));
            if (plateNumber.isPresent()) {
                log.info("Plate detected by GoogleVisionService [UUID: {}]: {}", uuid, plateNumber.get());
                return plateNumber;
            }

            boolean hasCar = (Boolean) googleVisionResponse.getOrDefault("hasCar", false);
            if (!hasCar) {
                log.debug("GoogleVisionService did not detect the car [UUID: {}]", uuid);
                return Optional.empty();
            }

            plateNumber = openAiVisionService.getPlateNumber(base64EncodedImage);
            if (plateNumber.isPresent()) {
                log.info("Plate detected by OpenAiVisionService [UUID: {}]: {}", uuid, plateNumber.get());
                return plateNumber;
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Error during plate detection [UUID: {}]", uuid, e);
            return Optional.empty();
        } finally {
            log.debug("Plate detection process completed [UUID: {}] in {} seconds", uuid, durationInSeconds(startTime));
            MDC.clear();
        }
    }

}
