package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.car.openai.OpenAiVisionService;
import ee.tenman.elektrihind.car.vision.GoogleVisionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PlateDetectionService {

    @Resource
    private GoogleVisionService googleVisionService;

    @Resource
    private OpenAiVisionService openAiVisionService;

    public Optional<String> detectPlate(byte[] image) {
        return openAiVisionService.getPlateNumber(image)
                .or(() -> googleVisionService.getPlateNumber(image));
    }

}
