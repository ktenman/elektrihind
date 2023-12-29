package ee.tenman.elektrihind.queue.redis;

import ee.tenman.elektrihind.queue.MessageDTO;
import ee.tenman.elektrihind.queue.QueueTextDetectionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "redis")
public class RedisMessageSubscriber implements MessageListener {

    @Resource
    private QueueTextDetectionService queueTextDetectionService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String messageBody = new String(message.getBody());
        log.debug("Received message from Redis queue: {}", messageBody);

        try {
            UUID uuid = extractUuidFromMessage(messageBody);
            String extractedTextFromImage = getExtractedTextFromImage(messageBody);
            log.info("Processing message [UUID: {}, Extracted text: {}]", uuid, extractedTextFromImage);

            MessageDTO messageDTO = MessageDTO.builder()
                    .uuid(uuid)
                    .text(extractedTextFromImage)
                    .build();

            queueTextDetectionService.processDetectionResponse(messageDTO);
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
