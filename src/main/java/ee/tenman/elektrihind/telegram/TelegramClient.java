package ee.tenman.elektrihind.telegram;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import static ee.tenman.elektrihind.telegram.TelegramClient.CLIENT_NAME;
import static ee.tenman.elektrihind.telegram.TelegramClient.CLIENT_URL;

@FeignClient(name = CLIENT_NAME, url = CLIENT_URL)
public interface TelegramClient {

    String CLIENT_NAME = "telegramClient";
    String CLIENT_URL = "https://api.telegram.org/";

    @GetMapping("bot{botToken}/sendMessage")
    String sendMessage(@PathVariable("botToken") String botToken,
                       @RequestParam("chat_id") String chatId,
                       @RequestParam("text") String message);

    @PostMapping(value = "bot{botToken}/sendDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String sendDocument(@PathVariable("botToken") String botToken,
                        @RequestParam("chat_id") String chatId,
                        @RequestPart("document") MultipartFile file);
}
