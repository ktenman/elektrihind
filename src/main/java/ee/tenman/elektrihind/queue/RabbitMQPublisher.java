package ee.tenman.elektrihind.queue;

import ee.tenman.elektrihind.config.rabbitmq.RabbitMQConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "rabbitmq")
public class RabbitMQPublisher implements MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(RedisMessage redisMessage) {
        log.debug("Publishing image request [UUID: {}]", redisMessage.getUuid());
        rabbitTemplate.convertAndSend(RabbitMQConstants.IMAGE_REQUEST_QUEUE, redisMessage.toString());
        log.info("Published image request [UUID: {}]", redisMessage.getUuid());
    }

    public void publishTextRequest(String textRequest, UUID uuid) {
        log.debug("Publishing text request [UUID: {}]", uuid);
        rabbitTemplate.convertAndSend(RabbitMQConstants.TEXT_REQUEST_QUEUE, uuid.toString() + ":" + textRequest);
        log.info("Published text request [UUID: {}]", uuid);
    }
}
