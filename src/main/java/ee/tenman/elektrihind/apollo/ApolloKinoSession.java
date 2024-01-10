package ee.tenman.elektrihind.apollo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static ee.tenman.elektrihind.apollo.ApolloKinoState.COMPLETED;
import static ee.tenman.elektrihind.apollo.ApolloKinoState.DECLINED;
import static ee.tenman.elektrihind.apollo.ApolloKinoState.INITIAL;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApolloKinoSession {

    private Integer sessionId;
    private LocalDateTime updatedAt = LocalDateTime.now();
    private ApolloKinoState currentState = ApolloKinoState.INITIAL;
    private Integer messageId;
    private Integer replyMessageId;
    private long chatId;
    private String selectedMovie;
    private LocalDate selectedDate;
    private LocalTime selectedTime;
    private List<StarSeat> selectedStarSeats = new ArrayList<>();
    private List<String> selectedOptions = new ArrayList<>();

    public ApolloKinoSession(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public void setSelectedSeat(String seat) {
        selectedStarSeats.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No star seat found"))
                .setSeat(seat);
    }

    @JsonIgnore
    public String getRowAndSeat() {
        return selectedStarSeats.stream()
                .findFirst()
                .map(StarSeat::getRowAndSeat)
                .orElseThrow(() -> new IllegalStateException("No row and seat selected"));
    }

    public void updateLastInteractionTime() {
        this.updatedAt = LocalDateTime.now();
    }

    public void setNextState() {
        if (currentState.isDeclined()) {
            return;
        }
        this.currentState = this.currentState.getNextState().orElse(COMPLETED);
        updateLastInteractionTime();
    }

    public void setPreviousState() {
        this.currentState = this.currentState.getPreviousState().orElse(INITIAL);
        updateLastInteractionTime();
    }

    public String getSelectedRow() {
        return selectedStarSeats.stream()
                .findFirst()
                .map(StarSeat::getRow)
                .orElseThrow(() -> new IllegalStateException("No row selected"));
    }

    @JsonIgnore
    public boolean isCompleted() {
        return currentState == COMPLETED;
    }

    @JsonIgnore
    public LocalDateTime getSelectedDateTime() {
        try {
            return LocalDateTime.of(selectedDate, selectedTime);
        } catch (Exception e) {
            return null;
        }
    }

    @JsonIgnore
    public String getPrompt(String... args) {
        return getCurrentState().getNextState().map(s -> s.getPrompt(args)).orElse("Prompt not found");
    }

    @JsonIgnore
    public void decline() {
        this.currentState = DECLINED;
    }

    @JsonIgnore
    public boolean isDeclined() {
        return this.currentState == DECLINED;
    }

    public void setSelectedRow(String row) {
        this.selectedStarSeats.add(StarSeat.builder().row(row).build());
    }
}
