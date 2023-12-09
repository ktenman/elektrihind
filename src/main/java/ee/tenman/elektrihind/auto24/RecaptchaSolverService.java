package ee.tenman.elektrihind.auto24;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@Slf4j
public class RecaptchaSolverService {

    private static final String SITE_KEY = "6Lf3qrkZAAAAAJLmqi1osY8lac0rLbAJItqEvZ0K";
    private static final String PAGE_URL = "https://www.auto24.ee/ostuabi/?t=soiduki-andmete-paring";
    @Resource
    private RecaptchaClient recaptchaClient;

    @Value("${twocaptcha.key}")
    private String apiKey;

    public String solveCaptcha() {
        return solveCaptcha(SITE_KEY, PAGE_URL);
    }

    public String solveCaptcha(String siteKey, String pageUrl) {
        Map<String, Object> response = recaptchaClient.sendCaptcha(apiKey, "userrecaptcha", siteKey, pageUrl, 1);
        String requestId = (String) response.get("request");

        for (int i = 0; i < 50; i++) {
            try {
                Thread.sleep(3000);
                Map<String, Object> captchaResponse = recaptchaClient.retrieveCaptcha(apiKey, "get", requestId, 1);
                if ((int) captchaResponse.get("status") == 1) {
                    return (String) captchaResponse.get("request");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }


    public String solveCaptcha(byte[] captchaImage) {
        String fileName = "captcha.png";
        String contentType = "image/png";
        MultipartFile multipartFile = new ByteArrayMultipartFile(captchaImage, "file", fileName, contentType);

        Map<String, Object> response = recaptchaClient.sendImageCaptcha(apiKey, "post", multipartFile, 1);
        String requestId = (String) response.get("request");

        for (int i = 0; i < 60; i++) {
            try {
                Thread.sleep(2000);
                Map<String, Object> captchaResponse = recaptchaClient.retrieveCaptcha(apiKey, "get", requestId, 1);
                if ((int) captchaResponse.get("status") == 1) {
                    return (String) captchaResponse.get("request");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }
}
