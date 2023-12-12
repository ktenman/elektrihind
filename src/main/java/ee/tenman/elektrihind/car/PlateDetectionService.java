package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.car.easyocr.EasyOcrService;
import ee.tenman.elektrihind.car.openai.OpenAiVisionService;
import ee.tenman.elektrihind.car.vision.GoogleVisionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

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
        UUID uuid = UUID.randomUUID();
        log.debug("Starting plate detection [UUID: {}]. Image size: {} bytes", uuid, image.length);
        try {
            Optional<String> plateNumber = easyOcrService.getPlateNumber(image);
            if (plateNumber.isPresent()) {
                log.info("Plate detected by EasyOcrService [UUID: {}]: {}", uuid, plateNumber.get());
                return plateNumber;
            }
            log.debug("EasyOcrService did not detect the plate [UUID: {}], trying GoogleVisionService", uuid);

            plateNumber = googleVisionService.getPlateNumber(image);
            if (plateNumber.isPresent()) {
                log.info("Plate detected by GoogleVisionService [UUID: {}]: {}", uuid, plateNumber.get());
                return plateNumber;
            }
            log.debug("GoogleVisionService did not detect the plate [UUID: {}], trying OpenAiVisionService", uuid);

            plateNumber = openAiVisionService.getPlateNumber(image);
            if (plateNumber.isPresent()) {
                log.info("Plate detected by OpenAiVisionService [UUID: {}]: {}", uuid, plateNumber.get());
                return plateNumber;
            }
            log.debug("OpenAiVisionService did not detect the plate [UUID: {}]", uuid);

            return Optional.empty();
        } catch (Exception e) {
            log.error("Error during plate detection [UUID: {}]", uuid, e);
            return Optional.empty();
        } finally {
            log.debug("Plate detection process completed [UUID: {}]", uuid);
        }
    }
}
