package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.car.easyocr.EasyOcrService;
import ee.tenman.elektrihind.car.openai.OpenAiVisionService;
import ee.tenman.elektrihind.car.vision.GoogleVisionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PlateDetectionService {

    @Resource
    private EasyOcrService easyOcrService;

    @Resource
    private GoogleVisionService googleVisionService;

    @Resource
    private OpenAiVisionService openAiVisionService;

    public Optional<String> detectPlate(byte[] image) {
        return easyOcrService.getPlateNumber(image)
                .or(() -> googleVisionService.getPlateNumber(image))
                .or(() -> openAiVisionService.getPlateNumber(image));
    }

}
