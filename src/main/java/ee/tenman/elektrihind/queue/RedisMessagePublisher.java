package ee.tenman.elektrihind.queue;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RedisMessagePublisher {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void publish(String queueName, RedisMessage redisMessage) {
        log.debug("Publishing message [UUID: {}] to queue [QueueName: {}]", redisMessage.getUuid(), queueName);
        stringRedisTemplate.convertAndSend(queueName, redisMessage.toString());
        log.info("Published message [UUID: {}] to queue [QueueName: {}]", redisMessage.getUuid(), queueName);
    }
}
