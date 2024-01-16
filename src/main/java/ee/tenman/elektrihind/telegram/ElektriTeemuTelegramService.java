package ee.tenman.elektrihind.telegram;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Service
@Slf4j
public class ElektriTeemuTelegramService {

    @Resource
    private TelegramClient telegramClient;

    @Value("${telegram.elektriteemu.token}")
    private String botToken;

    @Retryable(maxAttempts = 6, backoff = @Backoff(delay = 1500))
    public void sendToTelegram(String message, long chatId) {
        log.info("Sending message to chat: {} message: {}", chatId, message);
        telegramClient.sendMessage(botToken, String.valueOf(chatId), message);
        log.info("Message sent to chat: {}, message: {}", chatId, message);
    }

    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 7777))
    public void sendFileToTelegram(File file, long chatId) {
        log.info("Sending file to chat: {} file: {}", chatId, file.getName());
        MultipartFile multipartFile = new CustomMultipartFile(file);
        telegramClient.sendDocument(botToken, String.valueOf(chatId), multipartFile);
        log.info("File sent to chat: {}, file: {}", chatId, file.getName());
    }

}
