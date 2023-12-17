package ee.tenman.elektrihind.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "rabbitmq")
public class RabbitMQPublisher implements MessagePublisher {

    public static final String REQUEST_QUEUE = "picture-request-queue";
    private final RabbitTemplate rabbitTemplate;

    public RabbitMQPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(RedisMessage redisMessage) {
        log.debug("Publishing message [UUID: {}]", redisMessage.getUuid());
        rabbitTemplate.convertAndSend(REQUEST_QUEUE, redisMessage.toString());
        log.info("Published message [UUID: {}]", redisMessage.getUuid());
    }
}
