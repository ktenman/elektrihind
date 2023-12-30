package ee.tenman.elektrihind.car.predict;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static ee.tenman.elektrihind.car.predict.PredictClient.CLIENT_NAME;
import static ee.tenman.elektrihind.car.predict.PredictClient.CLIENT_URL;

@FeignClient(name = CLIENT_NAME, url = CLIENT_URL)
public interface PredictClient {

    String CLIENT_NAME = "predictClient";
    String CLIENT_URL = "http://localhost:55224";
//    String CLIENT_URL = "http://127.0.0.1:4000";

    @PostMapping(value = "/predict")
    PredictResponse predict(@RequestBody PredictRequest request);

}
