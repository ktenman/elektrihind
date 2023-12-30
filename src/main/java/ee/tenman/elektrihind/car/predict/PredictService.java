package ee.tenman.elektrihind.car.predict;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PredictService {

    @Resource
    private PredictClient predictClient;

    public PredictResponse predict(PredictRequest request) {
        return predictClient.predict(request);
    }

}
