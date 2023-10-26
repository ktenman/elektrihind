package ee.tenman.elektrihind;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "telegramClient", url = "https://api.telegram.org")
public interface TelegramClient {

    @GetMapping("/bot${telegram.botToken}/sendMessage")
    String sendMessage(@RequestParam("chat_id") String chatId, @RequestParam("text") String message);

}
