package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import static ee.tenman.elektrihind.electricity.ElectricityBotService.buildMD5;

@IntegrationTest
class PlateDetectionServiceTest {

    @Resource
    private PlateDetectionService plateDetectionService;

    @Test
    @Disabled
    void detectPlate() throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("image-detector/car1.jpg"));

        Base64.Encoder encoder = Base64.getEncoder();
        String string = encoder.encodeToString(bytes);
        String encodedImageMD5 = buildMD5(string);
        String plate = plateDetectionService.detectPlate(string, encodedImageMD5).orElseThrow();

         plate =  plateDetectionService.detectPlate(string, encodedImageMD5).orElseThrow();

        System.out.println();
    }

}
