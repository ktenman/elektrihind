package ee.tenman.elektrihind.telegram;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "telegramClient", url = "https://api.telegram.org")
public interface TelegramClient {

    @GetMapping("/bot${telegram.botToken}/sendMessage")
    String sendMessage(@RequestParam("chat_id") String chatId, @RequestParam("text") String message);

    @PostMapping(value = "/bot${telegram.botToken}/sendDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String sendDocument(@RequestParam("chat_id") String chatId, @RequestPart("document") MultipartFile file);

}
