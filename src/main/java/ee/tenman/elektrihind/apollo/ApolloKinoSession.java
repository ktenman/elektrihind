package ee.tenman.elektrihind.apollo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class ApolloKinoSession {
    private final UUID sessionId = UUID.randomUUID();
    private LocalDateTime lastUpdated = LocalDateTime.now();
    private ApolloKinoState currentState = ApolloKinoState.INITIAL;

    private String selectedMovie;
    private LocalDate selectedDate;
    private String selectedTime;
    private String selectedRow;
    private String selectedSeat;

    public void updateLastInteractionTime() {
        this.lastUpdated = LocalDateTime.now();
    }
}
