package ee.tenman.elektrihind.config;

import ee.tenman.elektrihind.queue.RabbitMQConsumer;
import ee.tenman.elektrihind.queue.RabbitMQPublisher;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "rabbitmq")
public class RabbitMQConfig {

    @Bean
    public Queue pictureRequestQueue() {
        return new Queue(RabbitMQPublisher.REQUEST_QUEUE, true);
    }

    @Bean
    public Queue pictureResponseQueue() {
        return new Queue(RabbitMQConsumer.RESPONSE_QUEUE, true);
    }

}
