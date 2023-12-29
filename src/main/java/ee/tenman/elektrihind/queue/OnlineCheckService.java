package ee.tenman.elektrihind.queue;

import ee.tenman.elektrihind.config.rabbitmq.RabbitMQConstants;
import ee.tenman.elektrihind.queue.rabbitmq.RabbitMQPublisher;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.messaging.type", havingValue = "rabbitmq")
public class OnlineCheckService {

    private static final int TIMEOUT = 200;
    private final Map<UUID, CompletableFuture<Boolean>> onlineCheckFutures = new ConcurrentHashMap<>();
    @Resource
    private RabbitMQPublisher rabbitMQPublisher;
    @Resource(name = "tenThreadExecutor")
    private ExecutorService executorService;

    public boolean isMacbookOnline() {
        UUID uuid = UUID.randomUUID();
        CompletableFuture<Boolean> onlineCheckFuture = new CompletableFuture<>();
        onlineCheckFutures.put(uuid, onlineCheckFuture);

        rabbitMQPublisher.publishOnlineCheckRequest(uuid);

        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return onlineCheckFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    log.warn("Timeout or interruption while waiting for online check response [UUID: {}]", uuid);
                    return false;
                }
            }, executorService).get();
        } catch (Exception e) {
            log.error("Error while awaiting online check response", e);
            return false;
        } finally {
            onlineCheckFutures.remove(uuid);
        }
    }

    @RabbitListener(queues = RabbitMQConstants.ONLINE_CHECK_RESPONSE_QUEUE)
    public void processOnlineCheckResponse(String message) {
        UUID uuid = UUID.fromString(message);
        CompletableFuture<Boolean> onlineCheckFuture = onlineCheckFutures.get(uuid);
        if (onlineCheckFuture == null) {
            log.warn("Received a response for an unknown or timed out online check [UUID: {}]", uuid);
            return;
        }
        onlineCheckFuture.complete(true);
    }
}
