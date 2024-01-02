package ee.tenman.elektrihind.car.predict;

import ee.tenman.elektrihind.utility.TimeUtility;
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
        long predictionStartTime = System.nanoTime();
        log.info("Predicting {}", request);
        try {
            PredictResponse predictResponse = captchaSolverClient.predict(request);
            String predictionDuration = TimeUtility.durationInSeconds(predictionStartTime).asString();
            log.info("Predicted {} in {} seconds", predictResponse, predictionDuration);
            return Optional.ofNullable(predictResponse).map(PredictResponse::getPredictedText);
        } catch (Exception e) {
            log.error("Failed to predict", e);
            return Optional.empty();
        }
    }

}
