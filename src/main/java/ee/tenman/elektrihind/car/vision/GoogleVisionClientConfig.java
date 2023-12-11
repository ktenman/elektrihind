package ee.tenman.elektrihind.car.vision;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

public class GoogleVisionClientConfig {
    @Bean
    public RequestInterceptor requestInterceptor(VisionAuthenticatorService authService) {
        return requestTemplate -> {
            String accessToken = authService.getAccessToken();
            requestTemplate.header("Authorization", "Bearer " + accessToken);
        };
    }
}
