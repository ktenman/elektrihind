package ee.tenman.elektrihind.car.predict;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static ee.tenman.elektrihind.car.predict.CaptchaSolverClient.CLIENT_NAME;

@FeignClient(name = CLIENT_NAME, url = "${captcha-solver.url}")
public interface CaptchaSolverClient {

    String CLIENT_NAME = "captchaSolverClient";

    @PostMapping(value = "/predict")
    PredictResponse predict(@RequestBody PredictRequest request);

}
