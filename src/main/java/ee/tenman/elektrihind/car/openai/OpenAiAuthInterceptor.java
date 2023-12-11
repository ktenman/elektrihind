package ee.tenman.elektrihind.car.openai;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenAiAuthInterceptor implements RequestInterceptor {

    @Value("${openai.token}")
    private String openaiToken;

    @Override
    public void apply(RequestTemplate template) {
        template.header("Authorization", "Bearer " + openaiToken);
    }
}
