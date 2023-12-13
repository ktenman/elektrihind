package ee.tenman.elektrihind.queue;

import ee.tenman.elektrihind.car.QueuePlateDetectionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    @Resource
    private QueuePlateDetectionService queuePlateDetectionService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String messageBody = new String(message.getBody());
        log.debug("Received message from Redis queue: {}", messageBody);

        try {
            UUID uuid = extractUuidFromMessage(messageBody);
            String extractedTextFromImage = getExtractedTextFromImage(messageBody);
            log.info("Processing message [UUID: {}, Extracted text: {}]", uuid, extractedTextFromImage);

            queuePlateDetectionService.processDetectionResponse(uuid, extractedTextFromImage);
        } catch (Exception e) {
            log.error("Error processing message from Redis queue", e);
        }
    }

    private UUID extractUuidFromMessage(String message) {
        try {
            return UUID.fromString(message.split(":")[0]);
        } catch (Exception e) {
            log.error("Error extracting UUID from message: {}", message, e);
            throw e;
        }
    }

    private String getExtractedTextFromImage(String message) {
        try {
            return message.split(":")[1];
        } catch (Exception e) {
            log.error("Error extracting text from message: {}", message, e);
            throw e;
        }
    }
}
