package ee.tenman.elektrihind.car.openai;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

import static ee.tenman.elektrihind.car.vision.GoogleVisionService.CAR_PLATE_NUMBER_PATTERN;

@Service
@Slf4j
public class OpenAiVisionService {

    @Resource
    private OpenAiClient openAiClient;

    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1500))
    public Optional<String> getPlateNumber(byte[] imageBytes) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        log.debug("Encoded image to base64");

        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, String> text = Map.of(
                "type", "text",
                "text", "What’s in this image? What are the numbers or text you see on the image?"
        );

        Map<String, Object> image = Map.of(
                "type", "image_url",
                "image_url", Map.of(
                        "url", "data:image/jpeg;base64," + base64Image)
        );

        messages.add(Map.of(
                "role", "user",
                "content", List.of(text, image)
        ));

        OpenAiRequest openAiRequest = new OpenAiRequest(messages);

        try {
            Optional<String> answer = openAiClient.askQuestion(openAiRequest).getAnswer();
            log.info("Received response from OpenAI");

            if (answer.isPresent()) {
                Matcher matcher = CAR_PLATE_NUMBER_PATTERN.matcher(answer.get());
                if (matcher.find()) {
                    String plateNumber = matcher.group().replace(" ", "").toUpperCase();
                    log.info("Extracted plate number: {}", plateNumber);
                    return Optional.of(plateNumber);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error processing OpenAI response", e);
            return Optional.empty();
        }
    }
}
