package ee.tenman.elektrihind.auto24;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "recaptchaClient", url = "http://2captcha.com")
public interface RecaptchaClient {

    @PostMapping("/in.php")
    Map<String, Object> sendCaptcha(@RequestParam("key") String apiKey,
                                    @RequestParam("method") String method,
                                    @RequestParam("googlekey") String siteKey,
                                    @RequestParam("pageurl") String pageUrl,
                                    @RequestParam("json") int json);

    @PostMapping("/res.php")
    Map<String, Object> retrieveCaptcha(@RequestParam("key") String apiKey,
                                        @RequestParam("action") String action,
                                        @RequestParam("id") String requestId,
                                        @RequestParam("json") int json);
}
