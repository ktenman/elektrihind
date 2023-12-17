package ee.tenman.elektrihind.queue;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static ee.tenman.elektrihind.car.googlevision.GoogleVisionService.CAR_PLATE_NUMBER_PATTERN;
import static ee.tenman.elektrihind.utility.TimeUtility.durationInSeconds;

@Service
@Slf4j
public class QueueTextDetectionService {

    private static final int TIMEOUT = 1500;
    private final Map<UUID, CompletableFuture<String>> plateDetectionFutures = new ConcurrentHashMap<>();
    @Resource
    private MessagePublisher messagePublisher;
    @Resource(name = "tenThreadExecutor")
    private ExecutorService executorService;

    public Optional<String> extractText(byte[] image) {
        long startTime = System.nanoTime();
        UUID uuid = UUID.randomUUID();
        MDC.put("uuid", uuid.toString());
        String base64EncodedImage = java.util.Base64.getEncoder().encodeToString(image);
        log.debug("Starting text extraction. Image size: {} bytes", base64EncodedImage.getBytes().length);

        try {
            Optional<String> plateNumber = detectPlate(base64EncodedImage, uuid, false);
            if (plateNumber.isPresent()) {
                log.info("Extracted text via queue: {}", plateNumber.get());
                return plateNumber;
            }

            log.debug("No text extracted in {} seconds", durationInSeconds(startTime));
            return Optional.empty();
        } finally {
            MDC.remove("uuid");
        }
    }

    public Optional<String> detectPlate(String base64EncodedImage, UUID uuid) {
        return detectPlate(base64EncodedImage, uuid, true);
    }

    public Optional<String> detectPlate(String base64EncodedImage, UUID uuid, boolean detectPlateNumber) {
        MDC.put("uuid", uuid.toString());
        long startTime = System.nanoTime();
        log.debug("Attempting plate detection via queue");
        CompletableFuture<String> detectionFuture = new CompletableFuture<>();
        plateDetectionFutures.put(uuid, detectionFuture);

        RedisMessage redisMessage = RedisMessage.builder()
                .base64EncodedImage(base64EncodedImage)
                .uuid(uuid)
                .build();

        messagePublisher.publish(redisMessage);

        try {
            String extractedText = CompletableFuture.supplyAsync(() -> {
                try {
                    return detectionFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException("Timeout or interruption while waiting for plate number [UUID: " + uuid + "]", e);
                }
            }, executorService).get();

            if (extractedText == null) {
                log.debug("No extracted text received from queue within timeout");
                return Optional.empty();
            }
            log.debug("Received extracted text from queue: {}", extractedText);

            if (!detectPlateNumber) {
                return Optional.of(extractedText);
            }

            Matcher matcher = CAR_PLATE_NUMBER_PATTERN.matcher(extractedText);
            if (matcher.find()) {
                String plateNr = matcher.group().replace(" ", "").toUpperCase();
                log.debug("Plate number found from queue: {} in {} seconds", plateNr, durationInSeconds(startTime));
                return Optional.of(plateNr);
            }

            log.debug("No plate number found from queue in {} seconds", durationInSeconds(startTime));
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error while awaiting plate detection response", e);
            return Optional.empty();
        } finally {
            plateDetectionFutures.remove(uuid);
            log.debug("Removed future from plate detection futures map");
            MDC.remove("uuid");
        }
    }

    public void processDetectionResponse(UUID uuid, String plateNumber) {
        MDC.put("uuid", uuid.toString());
        try {
            CompletableFuture<String> detectionFuture = plateDetectionFutures.get(uuid);
            if (detectionFuture == null) {
                log.warn("Received a response for an unknown or timed out request");
                return;
            }
            detectionFuture.complete(plateNumber);
            log.debug("Completed future with plate number: {}", plateNumber);
        } catch (Exception e) {
            log.error("Error while processing detection response", e);
        } finally {
            MDC.clear();
        }
    }
}
