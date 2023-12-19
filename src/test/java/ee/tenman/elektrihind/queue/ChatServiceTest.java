package ee.tenman.elektrihind.queue;

import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Optional;

@IntegrationTest
class ChatServiceTest {

    @Resource
    private ChatService chatService;

    @Test
    @Disabled
    void sendMessage() {
        Optional<String> response = chatService.sendMessage("2+3");

        System.out.println();
    }
}
