package ee.tenman.elektrihind.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.function.Supplier;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "rabbitmq")
public class RabbitMQPublisher implements MessagePublisher {

    private final Sinks.Many<Message<String>> sink = Sinks.many().multicast().directBestEffort();

    @Override
    public void publish(RedisMessage redisMessage) {
        log.debug("Publishing message [UUID: {}]", redisMessage.getUuid());
        Message<String> message = MessageBuilder.withPayload(redisMessage.toString()).build();
        sink.tryEmitNext(message);
        log.info("Published message [UUID: {}]", redisMessage.getUuid());
    }

    @Bean
    public Supplier<Flux<Message<String>>> rabbitmqPublisher() {
        return sink::asFlux;
    }
}
