package ee.tenman.elektrihind.car.easyocr;

import ee.tenman.elektrihind.car.predict.PredictRequest;
import ee.tenman.elektrihind.car.predict.PredictResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class EasyOcrService {

    @Resource
    EasyOcrClient easyOcrClient;

    public Optional<String> predict(String base64Image) {

        try {
            PredictResponse predictResponse = easyOcrClient.predict(new PredictRequest(base64Image));
            return Optional.of(predictResponse.getPredictedText())
                    .filter(StringUtils::isNotBlank)
                    .map(s -> s.replace(s, s.replaceAll("[^a-zA-Z0-9]", "")))
                    .map(String::toUpperCase)
                    .map(s -> s.substring(0, Math.min(s.length(), 4)));
        } catch (Exception e) {
            log.info("Failed to get prediction response from EasyOCR: ", e);
            return Optional.empty();
        }
    }
}
