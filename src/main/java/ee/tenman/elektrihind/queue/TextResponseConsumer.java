package ee.tenman.elektrihind.queue;

import ee.tenman.elektrihind.config.rabbitmq.RabbitMQConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "rabbitmq")
public class TextResponseConsumer {

    @Resource
    private ChatService chatService;

    @RabbitListener(queues = RabbitMQConstants.TEXT_RESPONSE_QUEUE)
    public void listen(String messageBody) {
        log.debug("Received text response from RabbitMQ queue: {}", messageBody);
        try {
            UUID uuid = UUID.fromString(messageBody.split(":")[0]);
            String responseText = messageBody.substring(messageBody.indexOf(':') + 1);
            log.info("Processing text response [UUID: {}, Response: {}]", uuid, responseText);

            chatService.processChatResponse(uuid, responseText);
        } catch (Exception e) {
            log.error("Error processing text response from RabbitMQ queue", e);
        }
    }
}
