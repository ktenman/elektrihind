package ee.tenman.elektrihind.queue.rabbitmq.consumer;

import ee.tenman.elektrihind.queue.MessageDTO;
import ee.tenman.elektrihind.queue.QueueTextDetectionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "rabbitmq")
public class PictureConsumer {

    public static final String RESPONSE_QUEUE = "picture-response-queue";

    @Resource
    private QueueTextDetectionService queueTextDetectionService;

    @RabbitListener(queues = RESPONSE_QUEUE)
    public void listen(String message) {
        MessageDTO messageDTO = MessageDTO.fromString(message);
        log.debug("Received message: {}", messageDTO);
        try {
            queueTextDetectionService.processDetectionResponse(messageDTO);
        } catch (Exception e) {
            log.error("Error processing message from RabbitMQ queue", e);
        }
    }

}
