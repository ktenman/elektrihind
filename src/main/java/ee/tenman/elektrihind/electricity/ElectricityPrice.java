package ee.tenman.elektrihind.electricity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import ee.tenman.elektrihind.config.CustomLocalDateTimeDeserializer;
import ee.tenman.elektrihind.utility.DateTimeConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectricityPrice implements Comparable<ElectricityPrice> {
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime date;
    private Double price;

    @Override
    public String toString() {
        return String.format("%s - %.2f%n", date.format(DateTimeConstants.DATE_TIME_FORMATTER), price);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElectricityPrice that = (ElectricityPrice) o;
        return Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date);
    }

    @Override
    public int compareTo(ElectricityPrice other) {
        return this.date.compareTo(other.date);
    }

}
