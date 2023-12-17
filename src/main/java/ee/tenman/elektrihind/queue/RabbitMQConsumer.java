package ee.tenman.elektrihind.queue;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "rabbitmq")
public class RabbitMQConsumer {

    @Resource
    private QueueTextDetectionService queueTextDetectionService;

    @Bean
    public Consumer<String> rabbitmqConsumer() {
        return messageBody -> {
            log.debug("Received message from RabbitMQ queue: {}", messageBody);
            try {
                UUID uuid = extractUuidFromMessage(messageBody);
                String extractedTextFromImage = getExtractedTextFromImage(messageBody);
                log.info("Processing message [UUID: {}, Extracted text: {}]", uuid, extractedTextFromImage);

                queueTextDetectionService.processDetectionResponse(uuid, extractedTextFromImage);
            } catch (Exception e) {
                log.error("Error processing message from RabbitMQ queue", e);
            }
        };
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
