package ee.tenman.elektrihind.queue;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@Slf4j
public class RedisMessagePublisher {

    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void publish(String queueName, RedisMessage redisMessage) {
        log.debug("Preparing to publish message to queue [QueueName: {}]", queueName);
        String message = createMessage(redisMessage);
        log.debug("Publishing message [UUID: {}] to queue [QueueName: {}]", redisMessage.getUuid(), queueName);
        stringRedisTemplate.convertAndSend(queueName, message);
        log.info("Published message [UUID: {}] to queue [QueueName: {}]", redisMessage.getUuid(), queueName);
    }

    private String createMessage(RedisMessage redisMessage) {
        String encodedImage = BASE64_ENCODER.encodeToString(redisMessage.getImageData());
        log.debug("Encoded image data for message [UUID: {}]", redisMessage.getUuid());
        return redisMessage.getUuid().toString() + ":" + encodedImage;
    }
}
