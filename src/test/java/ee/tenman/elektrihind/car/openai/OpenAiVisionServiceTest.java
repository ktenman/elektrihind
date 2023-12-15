package ee.tenman.elektrihind.car.openai;

import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@IntegrationTest
class OpenAiVisionServiceTest {

    @Resource
    private OpenAiVisionService openAiVisionService;

    @Test
    @Disabled
    void getPlateNumber() throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("image-detector/car2.jpg"));
        Base64.Encoder encoder = Base64.getEncoder();

        Optional<String> plateNumber = openAiVisionService.getPlateNumber(encoder.encodeToString(bytes), UUID.randomUUID());

        System.out.println();
    }
}
