package ee.tenman.elektrihind.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageDTO {

    private UUID uuid;
    private String text;

    /**
     * Static factory method to create an instance of MessageDTO from a string.
     * The string is expected to be in the format "UUID:text".
     *
     * @param message The message string to parse.
     * @return An instance of MessageDTO.
     */
    public static MessageDTO fromString(String message) {
        String[] parts = message.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Message format is invalid. Expected format 'UUID:text'");
        }
        UUID uuid = UUID.fromString(parts[0]);
        String text = parts[1];
        return new MessageDTO(uuid, text);
    }
}
