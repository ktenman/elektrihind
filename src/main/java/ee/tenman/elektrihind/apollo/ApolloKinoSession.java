package ee.tenman.elektrihind.apollo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static ee.tenman.elektrihind.apollo.ApolloKinoService.DATE_TIME_FORMATTER;

@Getter
public class ApolloKinoSession {
    private final Integer sessionId;
    private LocalDateTime lastUpdated = LocalDateTime.now();
    private ApolloKinoState currentState = ApolloKinoState.INITIAL;

    @Setter
    private Integer messageId;
    @Setter
    private Integer replyMessageId;
    @Setter
    private long chatId;

    private String selectedMovie;
    private String selectedDate;
    private String selectedTime;
    private String selectedRow;
    private String selectedSeat;

    public ApolloKinoSession(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public void updateLastInteractionTime() {
        this.lastUpdated = LocalDateTime.now();
    }

    public void updateCurrentState() {
        this.currentState = this.currentState.getNextState();
        updateLastInteractionTime();
    }

    public void setSelectedMovie(String selectedMovie) {
        this.selectedMovie = selectedMovie;
        updateCurrentState();
    }

    public void setSelectedDate(String selectedDate) {
        this.selectedDate = selectedDate;
        updateCurrentState();
    }

    public void setSelectedTime(String selectedTime) {
        this.selectedTime = selectedTime;
        updateCurrentState();
    }

    public void setSelectedRow(String selectedRow) {
        this.selectedRow = selectedRow;
        updateCurrentState();
    }

    public void setSelectedSeat(String selectedSeat) {
        this.selectedSeat = selectedSeat;
        updateCurrentState();
    }

    public String getKoht() {
        return selectedRow + "K" + selectedSeat;
    }

    public boolean isCompleted() {
        return currentState == ApolloKinoState.COMPLETED;
    }

    public LocalDateTime getSelectedDateTime() {
        try {
            return LocalDate.parse(selectedDate, DATE_TIME_FORMATTER)
                    .atTime(Integer.parseInt(selectedTime.split(":")[0]), Integer.parseInt(selectedTime.split(":")[1]));
        } catch (Exception e) {
            return null;
        }
    }

}
