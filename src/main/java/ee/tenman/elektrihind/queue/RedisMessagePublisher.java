package ee.tenman.elektrihind.queue;

import ee.tenman.elektrihind.config.RedisConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "redis")
public class RedisMessagePublisher implements MessagePublisher {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void publish(RedisMessage redisMessage) {
        String imageRequestQueue = RedisConfig.IMAGE_REQUEST_QUEUE;
        log.debug("Publishing message [UUID: {}] to queue [QueueName: {}]", redisMessage.getUuid(), imageRequestQueue);
        stringRedisTemplate.convertAndSend(imageRequestQueue, redisMessage.toString());
        log.info("Published message [UUID: {}] to queue [QueueName: {}]", redisMessage.getUuid(), imageRequestQueue);
    }
}
