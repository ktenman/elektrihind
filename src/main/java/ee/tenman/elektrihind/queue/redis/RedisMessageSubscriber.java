package ee.tenman.elektrihind.queue.redis;

import ee.tenman.elektrihind.queue.MessageDTO;
import ee.tenman.elektrihind.queue.QueueTextDetectionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "redis")
public class RedisMessageSubscriber implements MessageListener {

    @Resource
    private QueueTextDetectionService queueTextDetectionService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        MessageDTO messageDTO = MessageDTO.fromString(new String(message.getBody()));
        log.debug("Received message from Redis queue: {}", messageDTO);

        try {
            log.info("Processing message [UUID: {}, Extracted text: {}]", messageDTO.getUuid(), messageDTO.getText());
            queueTextDetectionService.processDetectionResponse(messageDTO);
        } catch (Exception e) {
            log.error("Error processing message from Redis queue", e);
        }
    }
}
