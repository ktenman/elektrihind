package ee.tenman.elektrihind.apollo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Optional;

public enum ApolloKinoState {
    INITIAL(""),
    SELECT_DATE("Select a day:"),
    SELECT_MOVIE("Select a movie:"),
    SELECT_TIME("Select a time:"),
    SELECT_ROW("Select a row:"),
    SELECT_SEAT("Select a seat in row %s:"),
    CONFIRMATION("Book: `%s` for `%s` on `%s` at `%s`?"),
    COMPLETED("Completed"),
    DECLINED("Declined");

    private final String prompt;

    ApolloKinoState(String prompt) {
        this.prompt = prompt;
    }

    @JsonIgnore
    public boolean isFinalState() {
        return this == COMPLETED || this == DECLINED;
    }

    @JsonIgnore
    public Optional<ApolloKinoState> getNextState() {
        if (isFinalState()) {
            return Optional.empty();
        }
        if (ordinal() + 1 < values().length) {
            return Optional.of(values()[ordinal() + 1]);
        }
        return Optional.empty();
    }

    public String getPrompt(String... args) {
        try {
            return String.format(prompt, (Object[]) args);
        } catch (java.util.MissingFormatArgumentException e) {
            return "Prompt formatting error for: " + this.name() + " with error: " + e.getMessage();
        }
    }

    @JsonIgnore
    public boolean isDeclined() {
        return this == DECLINED;
    }
}

