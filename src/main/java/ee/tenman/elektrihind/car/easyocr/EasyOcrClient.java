package ee.tenman.elektrihind.car.easyocr;

import ee.tenman.elektrihind.car.predict.PredictRequest;
import ee.tenman.elektrihind.car.predict.PredictResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static ee.tenman.elektrihind.car.easyocr.EasyOcrClient.CLIENT_NAME;
import static ee.tenman.elektrihind.car.easyocr.EasyOcrClient.CLIENT_URL;

@FeignClient(name = CLIENT_NAME, url = CLIENT_URL)
public interface EasyOcrClient {

    String CLIENT_NAME = "easyOcrClient";
    String CLIENT_URL = "http://localhost:55238";

    @PostMapping(value = "/predict")
    PredictResponse predict(@RequestBody PredictRequest request);

}

