package ee.tenman.elektrihind.car.easyocr;

import ee.tenman.elektrihind.IntegrationTest;
import ee.tenman.elektrihind.car.predict.PredictRequest;
import ee.tenman.elektrihind.utility.FileToBase64;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

@IntegrationTest
class EasyOcrServiceTest {

    @Resource
    EasyOcrService easyOcrService;

    @Test
    void predict() {
        String encodeToBase64 = FileToBase64.encodeToBase64("/Users/tenman/elektrihind/captcha_images_v2/4WP5.png");
        PredictRequest predictRequest = new PredictRequest(encodeToBase64);


        System.out.println();
    }
}
