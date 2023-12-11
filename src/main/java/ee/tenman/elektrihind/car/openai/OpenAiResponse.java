package ee.tenman.elektrihind.car.openai;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OpenAiResponse {
    private static final String CONTENT = "content";
    private static final String MESSAGE = "message";

    private List<Map<String, Object>> choices;

    public OpenAiResponse() {
    }

    public OpenAiResponse(List<Map<String, Object>> choices) {
        this.choices = choices;
    }

    public List<Map<String, Object>> getChoices() {
        return choices;
    }

    public void setChoices(List<Map<String, Object>> choices) {
        this.choices = choices;
    }

    public Optional<String> getAnswer() {
        if (choices == null || choices.isEmpty()) {
            return Optional.empty();
        }

        Object messageObject = choices.get(0).get(MESSAGE);
        if (!(messageObject instanceof Map<?, ?> messageMap)) {
            return Optional.empty();
        }

        Object contentObject = messageMap.get(CONTENT);
        if (!(contentObject instanceof String content)) {
            return Optional.empty();
        }

        return Optional.of(content);
    }


    @Override
    public String toString() {
        return "OpenAiResponse{" +
                "choices=" + choices +
                '}';
    }
}
