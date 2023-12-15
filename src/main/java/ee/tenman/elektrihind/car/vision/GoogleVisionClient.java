package ee.tenman.elektrihind.car.vision;

import feign.RequestInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static ee.tenman.elektrihind.car.vision.GoogleVisionClient.CLIENT_NAME;
import static ee.tenman.elektrihind.car.vision.GoogleVisionClient.CLIENT_URL;

@FeignClient(name = CLIENT_NAME, url = CLIENT_URL, configuration = GoogleVisionClient.Configuration.class)
public interface GoogleVisionClient {

    String CLIENT_NAME = "googleVisionClient";
    String CLIENT_URL = "https://vision.googleapis.com/v1";

    @PostMapping("/images:annotate")
    GoogleVisionApiResponse analyzeImage(@RequestBody GoogleVisionApiRequest requestBody);

    class Configuration {
        @Bean
        public RequestInterceptor requestInterceptor(VisionAuthenticatorService authService) {
            return requestTemplate -> requestTemplate.header("Authorization", "Bearer " + authService.getAccessToken());
        }
    }
}

