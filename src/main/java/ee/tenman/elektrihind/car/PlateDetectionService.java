package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.car.easyocr.EasyOcrService;
import ee.tenman.elektrihind.car.openai.OpenAiVisionService;
import ee.tenman.elektrihind.car.vision.GoogleVisionService;
import ee.tenman.elektrihind.config.RedisConfig;
import ee.tenman.elektrihind.queue.RedisMessage;
import ee.tenman.elektrihind.queue.RedisMessagePublisher;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PlateDetectionService {

    @Resource
    private EasyOcrService easyOcrService;

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
        log.debug("Starting plate detection [UUID: {}]. Image size: {} bytes", uuid, image.length);

        try {
            Optional<String> plateNumber = attemptPlateDetectionViaQueue(image, uuid);
            if (plateNumber.isPresent()) {
                return plateNumber;
            }

            Map<String, Object> googleVisionResponse = googleVisionService.getPlateNumber(image);
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

            plateNumber = openAiVisionService.getPlateNumber(image);
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

    private Optional<String> attemptPlateDetectionViaQueue(byte[] image, UUID uuid) {
        CompletableFuture<String> detectionFuture = new CompletableFuture<>();
        plateDetectionFutures.put(uuid, detectionFuture);

        RedisMessage redisMessage = RedisMessage.builder()
                .imageData(image)
                .uuid(uuid)
                .build();
        redisMessagePublisher.publish(RedisConfig.IMAGE_QUEUE, redisMessage);

        try {
            String plateNumber = CompletableFuture.supplyAsync(() -> {
                try {
                    return detectionFuture.get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException("Timeout or interruption while waiting for plate number", e);
                }
            }, executorService).get();

            return Optional.ofNullable(plateNumber);
        } catch (Exception e) {
            log.error("Error while awaiting plate detection response [UUID: {}]", uuid, e);
            return Optional.empty();
        } finally {
            plateDetectionFutures.remove(uuid);
        }
    }

    public void processDetectionResponse(UUID uuid, String plateNumber) {
        CompletableFuture<String> detectionFuture = plateDetectionFutures.get(uuid);
        if (detectionFuture != null) {
            detectionFuture.complete(plateNumber);
        } else {
            log.warn("Received a response for an unknown or timed out request [UUID: {}]", uuid);
        }
    }
}
