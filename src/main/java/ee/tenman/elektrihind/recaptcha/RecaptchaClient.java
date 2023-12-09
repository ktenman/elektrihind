package ee.tenman.elektrihind.recaptcha;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@FeignClient(name = "recaptchaClient", url = "http://2captcha.com")
public interface RecaptchaClient {


    @PostMapping(value = "/in.php", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String, Object> sendImageCaptcha(@RequestPart("key") String apiKey,
                                         @RequestPart("method") String method,
                                         @RequestPart("file") MultipartFile file,
                                         @RequestPart("json") int json);


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
