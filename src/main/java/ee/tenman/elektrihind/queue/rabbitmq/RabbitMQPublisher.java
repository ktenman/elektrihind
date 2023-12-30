package ee.tenman.elektrihind.queue.rabbitmq;

import ee.tenman.elektrihind.config.rabbitmq.RabbitMQConstants;
import ee.tenman.elektrihind.queue.MessagePublisher;
import ee.tenman.elektrihind.queue.redis.RedisMessage;
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

    public void publishImage(RedisMessage redisMessage) {
        publish(RabbitMQConstants.IMAGE_REQUEST_QUEUE, redisMessage.toString());
    }

    public void publishCaptcha(RedisMessage redisMessage) {
        publish(RabbitMQConstants.CAPTCHA_REQUEST_QUEUE, redisMessage.toString());
    }

    public void publishTextRequest(String textRequest, UUID uuid) {
        publish(RabbitMQConstants.TEXT_REQUEST_QUEUE, uuid.toString() + ":" + textRequest);
    }

    public void publishOnlineCheckRequest(UUID uuid) {
        publish(RabbitMQConstants.ONLINE_CHECK_REQUEST_QUEUE, uuid.toString());
    }

    private void publish(String queue, String message) {
        log.debug("Publishing message to queue [queue: {}]", queue);
        rabbitTemplate.convertAndSend(queue, message);
        log.info("Published message to queue [queue: {}]", queue);
    }

}
