package ee.tenman.elektrihind.car.predict;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class PredictService {

    @Resource
    private CaptchaSolverClient captchaSolverClient;

    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1500))
    public Optional<String> predict(PredictRequest request) {
        try {
            PredictResponse predictResponse = captchaSolverClient.predict(request);
            return Optional.ofNullable(predictResponse).map(PredictResponse::getPredictedText);
        } catch (Exception e) {
            log.error("Failed to predict", e);
            return Optional.empty();
        }
    }

}
