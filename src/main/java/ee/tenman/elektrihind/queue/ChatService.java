package ee.tenman.elektrihind.queue;

import ee.tenman.elektrihind.queue.rabbitmq.RabbitMQPublisher;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ChatService {

    private static final int TIMEOUT = 60000;
    private final Map<UUID, CompletableFuture<String>> chatFutures = new ConcurrentHashMap<>();
    @Resource
    private RabbitMQPublisher rabbitMQPublisher;
    @Resource(name = "tenThreadExecutor")
    private ExecutorService executorService;

    public Optional<String> sendMessage(String message) {
        UUID uuid = UUID.randomUUID();
        CompletableFuture<String> chatFuture = new CompletableFuture<>();
        chatFutures.put(uuid, chatFuture);

        rabbitMQPublisher.publishTextRequest(message, uuid);

        try {
            String response = CompletableFuture.supplyAsync(() -> {
                try {
                    return chatFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException("Timeout or interruption while waiting for chat response [UUID: " + uuid + "]", e);
                }
            }, executorService).get();

            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.error("Error while awaiting chat response", e);
            return Optional.empty();
        } finally {
            chatFutures.remove(uuid);
        }
    }

    public void processChatResponse(MessageDTO message) {
        CompletableFuture<String> chatFuture = chatFutures.get(message.getUuid());
        if (chatFuture == null) {
            log.warn("Received a response for an unknown or timed out request [UUID: {}]", message.getUuid());
            return;
        }
        chatFuture.complete(message.getText());
    }
}
