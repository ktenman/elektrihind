package ee.tenman.elektrihind.recaptcha;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@FeignClient(name = RecaptchaClient.CLIENT_NAME, url = RecaptchaClient.CLIENT_URL, configuration = RecaptchaClient.Configuration.class)
public interface RecaptchaClient {

    String CLIENT_NAME = "recaptchaClient";
    String CLIENT_URL = "http://2captcha.com";

    @PostMapping(value = "/in.php", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String, Object> submitImageCaptcha(
            @RequestPart("method") String method,
            @RequestPart("file") MultipartFile file
    );

    @PostMapping("/in.php")
    Map<String, Object> submitTextCaptcha(
            @RequestParam("method") String method,
            @RequestParam("googlekey") String siteKey,
            @RequestParam("pageurl") String pageUrl
    );

    @PostMapping("/res.php")
    Map<String, Object> retrieveCaptchaResult(
            @RequestParam("action") String action,
            @RequestParam("id") String requestId
    );

    class Configuration {
        @Value("${twocaptcha.key}")
        private String apiKey;

        @Bean
        public RequestInterceptor requestInterceptor() {
            return (RequestTemplate requestTemplate) -> {
                requestTemplate.query("key", apiKey);
                requestTemplate.query("json", "1");
            };
        }
    }
}
