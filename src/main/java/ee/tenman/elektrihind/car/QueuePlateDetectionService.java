package ee.tenman.elektrihind.car;

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
import java.util.regex.Matcher;

import static ee.tenman.elektrihind.car.vision.GoogleVisionService.CAR_PLATE_NUMBER_PATTERN;
import static ee.tenman.elektrihind.utility.TimeUtility.durationInSeconds;

@Service
@Slf4j
public class QueuePlateDetectionService {

    private static final int TIMEOUT = 1200;
    private final Map<UUID, CompletableFuture<String>> plateDetectionFutures = new ConcurrentHashMap<>();
    @Resource
    private RedisMessagePublisher redisMessagePublisher;
    @Resource(name = "tenThreadExecutor")
    private ExecutorService executorService;

    public Optional<String> detectPlate(String base64EncodedImage, UUID uuid) {
        long startTime = System.nanoTime();
        log.debug("Attempting plate detection via queue [UUID: {}]", uuid);
        CompletableFuture<String> detectionFuture = new CompletableFuture<>();
        plateDetectionFutures.put(uuid, detectionFuture);

        RedisMessage redisMessage = RedisMessage.builder()
                .base64EncodedImage(base64EncodedImage)
                .uuid(uuid)
                .build();

        redisMessagePublisher.publish(RedisConfig.IMAGE_REQUEST_QUEUE, redisMessage);

        try {
            String extractedText = CompletableFuture.supplyAsync(() -> {
                try {
                    return detectionFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
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
                log.debug("Plate number found from queue [UUID: {}]: {} in {} seconds", uuid, plateNr, durationInSeconds(startTime));
                return Optional.of(plateNr);
            }

            log.debug("No plate number found from queue [UUID: {}] in {} seconds", uuid, durationInSeconds(startTime));
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
