package ee.tenman.elektrihind.car.easyocr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class EasyOcrResponse {
    @JsonProperty("decoded_text")
    private String decodedText;
    private UUID uuid;
    private Instant timestamp;
}
