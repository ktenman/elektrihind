package ee.tenman.elektrihind.apollo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StarSeat {
    private String row;
    private String seat;

    @JsonIgnore
    public String getRowAndSeat() {
        return row + "K" + seat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StarSeat starSeat = (StarSeat) o;
        return Objects.equals(row, starSeat.row) && Objects.equals(seat, starSeat.seat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, seat);
    }

    @Override
    public String toString() {
        return getRowAndSeat();
    }
}
