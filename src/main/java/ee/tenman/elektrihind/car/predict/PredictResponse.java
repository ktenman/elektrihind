package ee.tenman.elektrihind.car.predict;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class PredictResponse {
    private UUID uuid;
    @JsonProperty("predicted_text")
    private String predictedText;
}
