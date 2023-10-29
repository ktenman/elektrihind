package ee.tenman.elektrihind.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    @Retryable(maxAttempts = 6, backoff = @Backoff(delay = 1500))
    public void sendCsvToTelegram(String filePath) {
        log.info("Sending CSV to Monitoring chat: {}", filePath);

        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();

        MultipartFile multipartFile = new MultipartFile() {
            @Override
            public String getName() {
                return "document";
            }

            @Override
            public String getOriginalFilename() {
                return fileName;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                try {
                    return Files.size(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public byte[] getBytes() throws IOException {
                return Files.readAllBytes(path);
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return Files.newInputStream(path);
            }

            @Override
            public Resource getResource() {
                return MultipartFile.super.getResource();
            }

            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                Files.copy(this.getInputStream(), dest.toPath());
            }

            @Override
            public void transferTo(Path dest) throws IOException, IllegalStateException {
                MultipartFile.super.transferTo(dest);
            }
        };

        telegramClient.sendDocument(monitoringChatId, multipartFile);
        log.info("CSV sent to Monitoring chat");
    }

}
