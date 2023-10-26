package ee.tenman.elektrihind;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ElectricityPrice {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime date;
    private Double price;

    @Override
    public String toString() {
        return String.format("%s - %.2f%n", date.format(Configs.DATE_TIME_FORMATTER), price);
    }
}
