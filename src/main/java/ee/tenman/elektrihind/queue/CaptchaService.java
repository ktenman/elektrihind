package ee.tenman.elektrihind.queue;

import ee.tenman.elektrihind.queue.rabbitmq.RabbitMQPublisher;
import ee.tenman.elektrihind.queue.redis.RedisMessage;
import ee.tenman.elektrihind.utility.FileToBase64;
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
public class CaptchaService {

    private static final int TIMEOUT = 4000;
    private final Map<UUID, CompletableFuture<String>> chatFutures = new ConcurrentHashMap<>();
    @Resource
    private RabbitMQPublisher rabbitMQPublisher;
    @Resource(name = "xThreadExecutor")
    private ExecutorService xThreadExecutor;

    public Optional<String> solve(byte[] captchaImage) {
        String encodedImage = FileToBase64.encodeToBase64(captchaImage);
        return solve(encodedImage);
    }

    public Optional<String> solve(String base64EncodedImage) {
        UUID uuid = UUID.randomUUID();
        CompletableFuture<String> chatFuture = new CompletableFuture<>();
        chatFutures.put(uuid, chatFuture);

        rabbitMQPublisher.publishCaptcha(RedisMessage.builder()
                .uuid(uuid)
                .base64EncodedImage(base64EncodedImage)
                .build());

        try {
            String response = CompletableFuture.supplyAsync(() -> {
                try {
                    return chatFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException("Timeout or interruption while waiting for chat response [UUID: " + uuid + "]", e);
                }
            }, xThreadExecutor).get();

            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.error("Error while awaiting chat response", e);
            return Optional.empty();
        } finally {
            chatFutures.remove(uuid);
        }
    }

    public void processResponse(MessageDTO message) {
        CompletableFuture<String> chatFuture = chatFutures.get(message.getUuid());
        if (chatFuture == null) {
            log.warn("Received a response for an unknown or timed out request [UUID: {}]", message.getUuid());
            return;
        }
        chatFuture.complete(message.getText());
    }
}
