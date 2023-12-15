package ee.tenman.elektrihind.twocaptcha;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TwoCaptchaSolverService {

    private static final String CAPTCHA_IMAGE_NAME = "captcha.png";
    private static final String CAPTCHA_IMAGE_TYPE = "image/png";
    private static final int MAX_RETRIES = 225;
    private static final long RETRY_DELAY_MS = 666;

    @Resource
    private TwoCaptchaClient twoCaptchaClient;

    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public String solveCaptcha(String siteKey, String pageUrl) {
        Map<String, Object> response = twoCaptchaClient.submitTextCaptcha("userrecaptcha", siteKey, pageUrl);
        String requestId = (String) response.get("request");
        return waitForCaptchaResult(requestId);
    }

    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public String solveCaptcha(byte[] captchaImage) {
        MultipartFile multipartFile = new ByteArrayMultipartFile(captchaImage, "file", CAPTCHA_IMAGE_NAME, CAPTCHA_IMAGE_TYPE);
        Map<String, Object> response = twoCaptchaClient.submitImageCaptcha("post", multipartFile);
        String requestId = (String) response.get("request");
        return waitForCaptchaResult(requestId);
    }

    private String waitForCaptchaResult(String requestId) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                Map<String, Object> captchaResponse = twoCaptchaClient.retrieveCaptchaResult("get", requestId);
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
