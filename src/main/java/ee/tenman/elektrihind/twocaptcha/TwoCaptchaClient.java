package ee.tenman.elektrihind.twocaptcha;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Map;

@FeignClient(name = TwoCaptchaClient.CLIENT_NAME, url = TwoCaptchaClient.CLIENT_URL, configuration = TwoCaptchaClient.Configuration.class)
public interface TwoCaptchaClient {

    String CLIENT_NAME = "recaptchaClient";
    String CLIENT_URL = "http://2captcha.com";

    @PostMapping(value = "/in.php", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String, Object> submitImageCaptcha(
            @RequestPart("method") String method,
            @RequestPart("file") MultipartFile file,
            @RequestPart("min_len") Integer minLen,
            @RequestPart("max_len") Integer maxLen,
            @RequestPart("language") Integer language // 0 - not specified, 1 - Cyrillic, 2 - Latin

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

    @Slf4j
    class Configuration {
        @Value("${twocaptcha.key}")
        private String apiKey;

        @Resource
        private Environment environment;

        @Bean
        public RequestInterceptor requestInterceptor() {
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isTestProfileActive = Arrays.asList(activeProfiles).contains("test");

            if (!isTestProfileActive && StringUtils.isBlank(apiKey)) {
                throw new RuntimeException("2Captcha API key not provided. Please provide a key in the 'twocaptcha.key' property.");
            } else if (StringUtils.isBlank(apiKey)) {
                log.warn("2Captcha API key not provided. Please provide a key in the 'twocaptcha.key' property.");
            }

            return (RequestTemplate requestTemplate) -> {
                requestTemplate.query("key", apiKey);
                requestTemplate.query("json", "1");
            };
        }
    }
}
