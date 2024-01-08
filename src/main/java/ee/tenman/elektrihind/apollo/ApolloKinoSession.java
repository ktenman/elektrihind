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
import static ee.tenman.elektrihind.apollo.ApolloKinoState.FAILED;
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
    private String selectedRow;
    private String selectedSeat;
    private List<String> selectedOptions = new ArrayList<>();

    public ApolloKinoSession(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public void updateLastInteractionTime() {
        this.updatedAt = LocalDateTime.now();
    }

    public void setNextState() {
        if (currentState.isDeclined() || currentState.isFailed()) {
            return;
        }
        this.currentState = this.currentState.getNextState().orElse(COMPLETED);
        updateLastInteractionTime();
    }

    public void setPreviousState() {
        this.currentState = this.currentState.getPreviousState().orElse(INITIAL);
        updateLastInteractionTime();
    }

    @JsonIgnore
    public String getRowAndSeat() {
        return selectedRow + "K" + selectedSeat;
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

    @JsonIgnore
    public void failed() {
        this.currentState = FAILED;
    }
}
