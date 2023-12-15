package ee.tenman.elektrihind.car.openai;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class OpenAiClientConfiguration {

    @Value("${openai.token}")
    private String openAiToken;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> template.header("Authorization", "Bearer " + openAiToken);
    }
}
