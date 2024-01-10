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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static ee.tenman.elektrihind.apollo.ApolloKinoState.COMPLETED;
import static ee.tenman.elektrihind.apollo.ApolloKinoState.DECLINED;
import static ee.tenman.elektrihind.apollo.ApolloKinoState.INITIAL;
import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

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
    private int seatCount;
    private Set<StarSeat> selectedStarSeats = new HashSet<>();
    private List<String> selectedOptions = new ArrayList<>();

    public ApolloKinoSession(Integer sessionId) {
        this.sessionId = sessionId;
    }

    @JsonIgnore
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

    private static String formatRange(List<Integer> seats) {
        if (seats.isEmpty()) return "";

        StringBuilder range = new StringBuilder();
        int start = seats.getFirst();
        int end = start;

        for (int i = 1; i < seats.size(); i++) {
            int current = seats.get(i);
            if (current == end + 1) {
                end = current;
            } else {
                range.append(formatRangeSegment(start, end)).append(", ");
                start = end = current;
            }
        }

        range.append(formatRangeSegment(start, end));
        return range.toString();
    }

    private static String formatRangeSegment(int start, int end) {
        return (start == end) ? String.valueOf(start) : start + "-" + end;
    }

    @JsonIgnore
    public String getRowAndSeats() {
        return Optional.of(selectedStarSeats).map(this::formatStarSeats).map(s -> "[" + s + "]").orElse("");
    }

    public String formatStarSeats(Collection<StarSeat> starSeats) {
        return starSeats.stream()
                .collect(Collectors.groupingBy(
                        StarSeat::getRow,
                        mapping(s -> parseInt(s.getSeat()), toList())
                ))
                .entrySet().stream()
                .map(entry -> {
                    List<Integer> seats = entry.getValue();
                    Collections.sort(seats);
                    return entry.getKey() + "K" + formatRange(seats);
                })
                .collect(joining(", "));
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

    @JsonIgnore
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
