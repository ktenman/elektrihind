package ee.tenman.elektrihind.car.automaks;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class AutomaksService {

    @Resource
    AutomaksClient automaksClient;
    
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public Optional<TaxResponse> calculate(CarDetails carDetails) {
        log.info("Calculating tax for car: {}", carDetails);
        try {
            return Optional.ofNullable(automaksClient.calculate(carDetails));
        } catch (Exception e) {
            log.error("Failed to get tax response from Automaks: ", e);
            return Optional.empty();
        }
    }

}
