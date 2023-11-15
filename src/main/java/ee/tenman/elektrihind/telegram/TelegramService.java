package ee.tenman.elektrihind.telegram;

import ee.tenman.elektrihind.electricity.ElectricityPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TelegramService {

    @Autowired
    private TelegramClient telegramClient;

    @Value("${telegram.monitoringChatId}")
    private String monitoringChatId;

    @Value("${telegram.privateChatId}")
    private String privateChatId;

    @Autowired
    private Clock clock;

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

    public String formatPricesForTelegram(List<ElectricityPrice> filteredPrices) {
        StringBuilder builder = new StringBuilder();
        filteredPrices.forEach(builder::append);

        Optional<ElectricityPrice> mostExpensiveHour = filteredPrices.stream().max(Comparator.comparingDouble(ElectricityPrice::getPrice));
        Optional<ElectricityPrice> cheapestHour = filteredPrices.stream().min(Comparator.comparingDouble(ElectricityPrice::getPrice));

        builder.append("\n");
        mostExpensiveHour.ifPresent(p -> {
            builder.append(priceDateLabel(p.getDate())).append("the most expensive: ").append(p);
            log.debug("Most expensive price: {}", p);
        });
        cheapestHour.ifPresent(p -> {
            builder.append(priceDateLabel(p.getDate())).append("the cheapest: ").append(p);
            log.debug("Cheapest price: {}", p);
        });

        return builder.toString();
    }

    String priceDateLabel(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (dateTime.toLocalDate().isEqual(now.toLocalDate())) {
            return "Today, ";
        } else if (dateTime.toLocalDate().isEqual(now.plusDays(1).toLocalDate())) {
            return "Tomorrow, ";
        } else {
            return "";
        }
    }

}
