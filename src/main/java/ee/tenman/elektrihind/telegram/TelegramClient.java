package ee.tenman.elektrihind.telegram;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import static ee.tenman.elektrihind.telegram.TelegramClient.CLIENT_NAME;
import static ee.tenman.elektrihind.telegram.TelegramClient.CLIENT_URL;

@FeignClient(name = CLIENT_NAME, url = CLIENT_URL, configuration = TelegramClient.Configuration.class)
public interface TelegramClient {

    String CLIENT_NAME = "telegramClient";
    String CLIENT_URL = "https://api.telegram.org/bot${telegram.botToken}/";

    @GetMapping("sendMessage")
    String sendMessage(@RequestParam("text") String message);

    @PostMapping(value = "sendDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String sendDocument(@RequestPart("document") MultipartFile file);


    class Configuration {
        @Value("${telegram.monitoringChatId}")
        private String monitoringChatId;

        @Bean
        public RequestInterceptor requestInterceptor() {
            return template -> template.query("chat_id", monitoringChatId);
        }
    }

}
