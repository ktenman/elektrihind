package ee.tenman.elektrihind.queue.rabbitmq.consumer;

import ee.tenman.elektrihind.config.rabbitmq.RabbitMQConstants;
import ee.tenman.elektrihind.queue.ChatService;
import ee.tenman.elektrihind.queue.MessageDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "rabbitmq")
public class TextConsumer {

    @Resource
    private ChatService chatService;

    @RabbitListener(queues = RabbitMQConstants.TEXT_RESPONSE_QUEUE)
    public void listen(String message) {
        MessageDTO messageDTO = MessageDTO.fromString(message);
        log.debug("Received text response from RabbitMQ queue: {}", messageDTO);
        try {
            chatService.processChatResponse(messageDTO);
        } catch (Exception e) {
            log.error("Error processing text response from RabbitMQ queue", e);
        }
    }
}
