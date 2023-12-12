package ee.tenman.elektrihind.car.easyocr;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class EasyOcrRequest {
    private String image;
    private UUID uuid = UUID.randomUUID();
    private Instant timestamp = Instant.now();

    public EasyOcrRequest(String image) {
        this.image = image;
    }
}
