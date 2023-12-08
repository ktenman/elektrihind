package ee.tenman.elektrihind.digitalocean;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;

public class AuthRequestInterceptor implements RequestInterceptor {

    private final String authToken;

    public AuthRequestInterceptor(@Value("${digitalocean.api.token}") String authToken) {
        this.authToken = authToken;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header("Authorization", "Bearer " + authToken);
    }
}
