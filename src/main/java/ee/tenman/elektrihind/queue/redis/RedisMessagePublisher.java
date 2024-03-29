package ee.tenman.elektrihind.queue.redis;

import ee.tenman.elektrihind.config.RedisConfiguration;
import ee.tenman.elektrihind.queue.MessagePublisher;
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
    public void publishImage(RedisMessage redisMessage) {
        String imageRequestQueue = RedisConfiguration.IMAGE_REQUEST_QUEUE;
        log.debug("Publishing message [UUID: {}] to queue [QueueName: {}]", redisMessage.getUuid(), imageRequestQueue);
        stringRedisTemplate.convertAndSend(imageRequestQueue, redisMessage.toString());
        log.info("Published message [UUID: {}] to queue [QueueName: {}]", redisMessage.getUuid(), imageRequestQueue);
    }
}
