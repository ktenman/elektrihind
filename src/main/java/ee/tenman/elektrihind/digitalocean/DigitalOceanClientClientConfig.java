package ee.tenman.elektrihind.digitalocean;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class DigitalOceanClientClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor(@Value("${digitalocean.api.token}") String authToken) {
        return new AuthRequestInterceptor(authToken);
    }
}
