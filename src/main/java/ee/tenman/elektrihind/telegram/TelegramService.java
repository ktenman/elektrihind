package ee.tenman.elektrihind.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TelegramService {

    private final TelegramClient telegramClient;

    @Value("${telegram.monitoringChatId}")
    private String monitoringChatId;

    @Value("${telegram.privateChatId}")
    private String privateChatId;

    public TelegramService(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    public void sendToTelegram(String message) {
        telegramClient.sendMessage(monitoringChatId, message);
//        telegramClient.sendMessage(privateChatId, message);
    }

}
