package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.car.openai.OpenAiVisionService;
import ee.tenman.elektrihind.car.vision.GoogleVisionService;
import ee.tenman.elektrihind.config.RedisConfig;
import ee.tenman.elektrihind.queue.RedisMessage;
import ee.tenman.elektrihind.queue.RedisMessagePublisher;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static ee.tenman.elektrihind.car.vision.GoogleVisionService.CAR_PLATE_NUMBER_PATTERN;

@Service
@Slf4j
public class PlateDetectionService {

    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

    @Resource
    private GoogleVisionService googleVisionService;

    @Resource
    private OpenAiVisionService openAiVisionService;

    private final Map<UUID, CompletableFuture<String>> plateDetectionFutures = new ConcurrentHashMap<>();
    @Resource(name = "tenThreadExecutor")
    private ExecutorService executorService;
    @Resource
    private RedisMessagePublisher redisMessagePublisher;

    public Optional<String> detectPlate(byte[] image) {
        UUID uuid = UUID.randomUUID();
        String base64EncodedImage = BASE64_ENCODER.encodeToString(image);
        log.debug("Starting plate detection [UUID: {}]. Image size: {} bytes", uuid, base64EncodedImage.getBytes().length);

        try {
            Optional<String> plateNumber = attemptPlateDetectionViaQueue(base64EncodedImage, uuid);
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
            log.debug("Plate detection process completed [UUID: {}]", uuid);
        }
    }

    private Optional<String> attemptPlateDetectionViaQueue(String base64EncodedImage, UUID uuid) {
        log.debug("Attempting plate detection via queue [UUID: {}]", uuid);
        CompletableFuture<String> detectionFuture = new CompletableFuture<>();
        plateDetectionFutures.put(uuid, detectionFuture);

        RedisMessage redisMessage = RedisMessage.builder()
                .base64EncodedImage(base64EncodedImage)
                .uuid(uuid)
                .build();

        redisMessagePublisher.publish(RedisConfig.IMAGE_QUEUE, redisMessage);

        try {
            String extractedText = CompletableFuture.supplyAsync(() -> {
                try {
                    return detectionFuture.get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException("Timeout or interruption while waiting for plate number [UUID: " + uuid + "]", e);
                }
            }, executorService).get();

            if (extractedText == null) {
                log.debug("No plate number received from queue within timeout [UUID: {}]", uuid);
                return Optional.empty();
            }
            log.debug("Received extracted text from queue [UUID: {}]: {}", uuid, extractedText);

            Matcher matcher = CAR_PLATE_NUMBER_PATTERN.matcher(extractedText);
            if (matcher.find()) {
                String plateNr = matcher.group().replace(" ", "").toUpperCase();
                log.debug("Plate number found from queue [UUID: {}]: {}", uuid, plateNr);
                return Optional.of(plateNr);
            }

            log.debug("No plate number found from queue [UUID: {}]", uuid);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error while awaiting plate detection response [UUID: {}]", uuid, e);
            return Optional.empty();
        } finally {
            plateDetectionFutures.remove(uuid);
            log.debug("Removed future from plate detection futures map [UUID: {}]", uuid);
        }
    }

    public void processDetectionResponse(UUID uuid, String plateNumber) {
        CompletableFuture<String> detectionFuture = plateDetectionFutures.get(uuid);
        if (detectionFuture == null) {
            log.warn("Received a response for an unknown or timed out request [UUID: {}]", uuid);
            return;
        }
        detectionFuture.complete(plateNumber);
        log.debug("Completed future with plate number [UUID: {}]: {}", uuid, plateNumber);
    }
}
