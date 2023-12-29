package ee.tenman.elektrihind.queue;

import ee.tenman.elektrihind.queue.redis.RedisMessage;

public interface MessagePublisher {
    void publishImage(RedisMessage redisMessage);
}
