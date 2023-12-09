package ee.tenman.elektrihind.recaptcha;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RecaptchaSolverService {

    private static final String CAPTCHA_IMAGE_NAME = "captcha.png";
    private static final String CAPTCHA_IMAGE_TYPE = "image/png";
    private static final int MAX_RETRIES = 60;
    private static final long RETRY_DELAY_MS = 2500;

    @Resource
    private RecaptchaClient recaptchaClient;

    @Value("${twocaptcha.key}")
    private String apiKey;

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public String solveCaptcha(String siteKey, String pageUrl) {
        Map<String, Object> response = recaptchaClient.sendCaptcha(apiKey, "userrecaptcha", siteKey, pageUrl, 1);
        String requestId = (String) response.get("request");
        return waitForCaptchaResult(requestId);
    }

    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000))
    public String solveCaptcha(byte[] captchaImage) {
        MultipartFile multipartFile = new ByteArrayMultipartFile(captchaImage, "file", CAPTCHA_IMAGE_NAME, CAPTCHA_IMAGE_TYPE);
        Map<String, Object> response = recaptchaClient.sendImageCaptcha(apiKey, "post", multipartFile, 1);
        String requestId = (String) response.get("request");
        return waitForCaptchaResult(requestId);
    }

    private String waitForCaptchaResult(String requestId) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                Map<String, Object> captchaResponse = recaptchaClient.retrieveCaptcha(apiKey, "get", requestId, 1);
                if ((int) captchaResponse.get("status") == 1) {
                    return (String) captchaResponse.get("request");
                }
            } catch (InterruptedException e) {
                log.error("Thread interrupted", e);
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                log.error("Error while retrieving captcha", e);
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Captcha not solved in time");
    }
}
