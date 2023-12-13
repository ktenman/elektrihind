package ee.tenman.elektrihind.car;

import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
}
