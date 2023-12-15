package ee.tenman.elektrihind.car.openai;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static ee.tenman.elektrihind.car.openai.OpenAiClient.CLIENT_NAME;
import static ee.tenman.elektrihind.car.openai.OpenAiClient.CLIENT_URL;

@FeignClient(name = CLIENT_NAME, url = CLIENT_URL, configuration = OpenAiClient.Configuration.class)
public interface OpenAiClient {

    String CLIENT_NAME = "openAiClient";
    String CLIENT_URL = "https://api.openai.com";

    @PostMapping(value = "/v1/chat/completions")
    OpenAiResponse askQuestion(@RequestBody OpenAiRequest request);

    class Configuration {
        @Value("${openai.token}")
        private String openAiToken;

        @Bean
        public RequestInterceptor requestInterceptor() {
            return template -> template.header("Authorization", "Bearer " + openAiToken);
        }
    }
}
