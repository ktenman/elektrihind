package ee.tenman.elektrihind.queue;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.AbstractMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConsumerConfig {

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new CustomMessageConverter());
        return factory;
    }

    static class CustomMessageConverter extends AbstractMessageConverter {

        @Override
        protected Message createMessage(Object object, MessageProperties messageProperties) {
            throw new UnsupportedOperationException("This converter does not support outbound messages");
        }

        @Override
        public Object fromMessage(Message message) {
            String messageBody = new String(message.getBody());
            return MessageDTO.fromString(messageBody);
        }
    }

}
