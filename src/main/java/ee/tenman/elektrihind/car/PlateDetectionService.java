package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.car.googlevision.GoogleVisionService;
import ee.tenman.elektrihind.car.openai.OpenAiVisionService;
import ee.tenman.elektrihind.config.RedisConfiguration;
import ee.tenman.elektrihind.queue.QueueTextDetectionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static ee.tenman.elektrihind.utility.TimeUtility.durationInSeconds;

@Service
@Slf4j
public class PlateDetectionService {

    public static final String PLATE_NUMBER = "plateNumber";

    @Resource
    private GoogleVisionService googleVisionService;

    @Resource
    private OpenAiVisionService openAiVisionService;

    @Resource
    QueueTextDetectionService queueTextDetectionService;

    @Cacheable(value = RedisConfiguration.ONE_YEAR_CACHE_1, key = "#imageHashValue")
    public Optional<String> detectPlate(String base64EncodedImage, String imageHashValue) {
        long startTime = System.nanoTime();
        UUID uuid = UUID.randomUUID();
        MDC.put("uuid", uuid.toString());
        log.debug("Starting plate detection. Image size: {} bytes", base64EncodedImage.getBytes().length);

        try {
            Optional<String> plateNumber = queueTextDetectionService.detectPlate(base64EncodedImage, uuid);
            if (plateNumber.isPresent()) {
                log.info("Plate detected via queue: {}", plateNumber.get());
                return plateNumber;
            }

            Map<String, String> googleVisionResponse = googleVisionService.getPlateNumber(base64EncodedImage, uuid);
            if (googleVisionResponse.containsKey(PLATE_NUMBER)) {
                log.info("Plate detected by GoogleVisionService: {}", googleVisionResponse.get(PLATE_NUMBER));
                return Optional.of(googleVisionResponse.get(PLATE_NUMBER));
            }

            boolean hasCar = Optional.ofNullable(googleVisionResponse.get("hasCar"))
                    .map(Boolean::valueOf)
                    .orElse(false);
            if (!hasCar) {
                log.debug("GoogleVisionService did not detect the car");
                return Optional.empty();
            }

            plateNumber = openAiVisionService.getPlateNumber(base64EncodedImage, uuid);
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
