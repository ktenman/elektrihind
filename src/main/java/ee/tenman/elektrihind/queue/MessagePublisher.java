package ee.tenman.elektrihind.queue;

public interface MessagePublisher {
    void publish(RedisMessage redisMessage);
}
