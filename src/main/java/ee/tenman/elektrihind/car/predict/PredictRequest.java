package ee.tenman.elektrihind.car.predict;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
public class PredictRequest {

    @Builder.Default
    private UUID uuid = UUID.randomUUID();
    @JsonProperty("base64_image")
    private String base64Image;

    public PredictRequest(String base64Image) {
        this.base64Image = base64Image;
    }

    @Override
    public String toString() {
        int length = this.getBase64Image().length();
        return "PredictRequest(uuid=" + this.getUuid() + ", base64Image=" + this.getBase64Image().substring(0, 5) + "..." +
                this.getBase64Image().substring(length - 6, length - 1) + ")";
    }
}
