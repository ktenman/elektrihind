package ee.tenman.elektrihind.queue.rabbitmq.consumer;

import ee.tenman.elektrihind.config.rabbitmq.RabbitMQConstants;
import ee.tenman.elektrihind.queue.CaptchaService;
import ee.tenman.elektrihind.queue.MessageDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "rabbitmq")
public class CaptchaConsumer {

    @Resource
    private CaptchaService captchaService;

    @RabbitListener(queues = RabbitMQConstants.CAPTCHA_RESPONSE_QUEUE)
    public void listen(String message) {
        log.info("Received message: {}", message);
        MessageDTO messageDTO = MessageDTO.fromString(message);
        try {
            captchaService.processResponse(messageDTO);
        } catch (Exception e) {
            log.error("Error processing message from RabbitMQ queue", e);
        }
    }

}
