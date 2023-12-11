package ee.tenman.elektrihind.car.vision;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "googleVisionClient", url = "https://vision.googleapis.com/v1", configuration = GoogleVisionClientConfig.class)
public interface GoogleVisionClient {
    @PostMapping("/images:annotate")
    GoogleVisionApiResponse analyzeImage(@RequestBody GoogleVisionApiRequest requestBody);
}

