package ee.tenman.elektrihind.car.openai;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "openAiClient", url = "https://api.openai.com")
public interface OpenAiClient {

    @PostMapping("/v1/chat/completions")
    OpenAiResponse askQuestion(@RequestBody OpenAiRequest request);

}
