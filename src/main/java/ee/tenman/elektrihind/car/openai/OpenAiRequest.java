package ee.tenman.elektrihind.car.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class OpenAiRequest {
    private static final String DEFAULT_MODEL = "gpt-4-vision-preview";
    private static final String ROLE = "role";
    private static final String USER = "user";
    private static final String CONTENT = "content";

    private String model;
    private List<Map<String, Object>> messages;
    @JsonProperty("max_tokens")
    private int maxTokens = 300;

    public OpenAiRequest(List<Map<String, Object>> messages) {
        this.model = DEFAULT_MODEL;
        this.messages = messages;
    }
}
