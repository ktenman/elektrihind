package ee.tenman.elektrihind.queue.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisMessage {
    String base64EncodedImage;
    private UUID uuid;

    @Override
    public String toString() {
        return this.uuid.toString() + ":" + this.base64EncodedImage;
    }
}
