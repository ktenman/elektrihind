package ee.tenman.elektrihind.queue;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "rabbitmq")
public class RabbitMQConsumer {

    public static final String RESPONSE_QUEUE = "picture-response-queue";

    @Resource
    private QueueTextDetectionService queueTextDetectionService;

    @RabbitListener(queues = RESPONSE_QUEUE)
    public void listen(MessageDTO message) {
        log.debug("Received message: {}", message);
        try {
            queueTextDetectionService.processDetectionResponse(message.getUuid(), message.getText());
        } catch (Exception e) {
            log.error("Error processing message from RabbitMQ queue", e);
        }
    }

}
