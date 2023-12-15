package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

@IntegrationTest
class PlateDetectionServiceTest {

    @Resource
    private PlateDetectionService plateDetectionService;

    @Test
    @Disabled
    void detectPlate() throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("image-detector/car2.jpg"));

        String plate = plateDetectionService.detectPlate(bytes).orElseThrow();

        System.out.println();
    }

    @Test
    @Disabled
    void detectPlate2() throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("image-detector/car2.jpg"));

        Base64.Encoder encoder = Base64.getEncoder();

        String base64EncodedImage = encoder.encodeToString(bytes);

        String plate = plateDetectionService.buildMD5(base64EncodedImage);

        System.out.println();
    }

}
