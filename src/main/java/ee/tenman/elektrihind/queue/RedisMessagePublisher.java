package ee.tenman.elektrihind.queue;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class RedisMessagePublisher {

    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void publish(String queueName, RedisMessage redisMessage) {
        String message = createMessage(redisMessage);
        stringRedisTemplate.convertAndSend(queueName, message);
    }

    private String createMessage(RedisMessage redisMessage) {
        return redisMessage.getUuid().toString() + ":" + BASE64_ENCODER.encodeToString(redisMessage.getImageData());
    }
}
