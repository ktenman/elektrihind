package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.car.easyocr.EasyOcrService;
import ee.tenman.elektrihind.car.openai.OpenAiVisionService;
import ee.tenman.elektrihind.car.vision.GoogleVisionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class PlateDetectionService {

    @Resource
    private EasyOcrService easyOcrService;

    @Resource
    private GoogleVisionService googleVisionService;

    @Resource
    private OpenAiVisionService openAiVisionService;

    public Optional<String> detectPlate(byte[] image) {
        log.debug("Starting plate detection. Image size: {} bytes", image.length);
        try {
            Optional<String> plateNumber = easyOcrService.getPlateNumber(image);
            if (plateNumber.isPresent()) {
                log.info("Plate detected by EasyOcrService: {}", plateNumber.get());
                return plateNumber;
            }
            log.debug("EasyOcrService did not detect the plate, trying GoogleVisionService");

            plateNumber = googleVisionService.getPlateNumber(image);
            if (plateNumber.isPresent()) {
                log.info("Plate detected by GoogleVisionService: {}", plateNumber.get());
                return plateNumber;
            }
            log.debug("GoogleVisionService did not detect the plate, trying OpenAiVisionService");

            plateNumber = openAiVisionService.getPlateNumber(image);
            if (plateNumber.isPresent()) {
                log.info("Plate detected by OpenAiVisionService: {}", plateNumber.get());
                return plateNumber;
            }
            log.debug("OpenAiVisionService did not detect the plate");

            return Optional.empty();
        } catch (Exception e) {
            log.error("Error during plate detection", e);
            return Optional.empty();
        } finally {
            log.debug("Plate detection process completed");
        }
    }
}
