package ee.tenman.elektrihind.electricity;

import com.fasterxml.jackson.annotation.JsonFormat;
import ee.tenman.elektrihind.util.DateTimeConstants;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ElectricityPrice {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime date;
    private Double price;

    @Override
    public String toString() {
        return String.format("%s - %.2f%n", date.format(DateTimeConstants.DATE_TIME_FORMATTER), price);
    }
}
