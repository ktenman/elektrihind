package ee.tenman.elektrihind.config.rabbitmq;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "rabbitmq")
public class RabbitMQConfig {

    @Bean
    public Queue pictureRequestQueue() {
        return new Queue(RabbitMQConstants.IMAGE_REQUEST_QUEUE, true);
    }

    @Bean
    public Queue pictureResponseQueue() {
        return new Queue(RabbitMQConstants.IMAGE_RESPONSE_QUEUE, true);
    }

    @Bean
    public Queue textRequestQueue() {
        return new Queue(RabbitMQConstants.TEXT_REQUEST_QUEUE, true);
    }

    @Bean
    public Queue textResponseQueue() {
        return new Queue(RabbitMQConstants.TEXT_RESPONSE_QUEUE, true);
    }
}

