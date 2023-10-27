package ee.tenman.elektrihind.telegram;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TelegramService {

    private final TelegramClient telegramClient;

    @Value("${telegram.monitoringChatId}")
    private String monitoringChatId;

    @Value("${telegram.privateChatId}")
    private String privateChatId;

    public TelegramService(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @Retryable(maxAttempts = 6, backoff = @Backoff(delay = 1500))
    public void sendToTelegram(String message) {
//        log.info("Sending message to private chat: {}", message);
//        telegramClient.sendMessage(privateChatId, message);
//        log.info("Message sent to private chat");

        log.info("Sending message to Monitoring chat: {}", message);
        telegramClient.sendMessage(monitoringChatId, message);
        log.info("Message sent to Monitoring chat");
    }

}
