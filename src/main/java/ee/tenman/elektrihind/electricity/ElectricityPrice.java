package ee.tenman.elektrihind.electricity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import ee.tenman.elektrihind.config.CustomLocalDateTimeDeserializer;
import ee.tenman.elektrihind.util.DateTimeConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectricityPrice {
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime date;
    private Double price;

    @Override
    public String toString() {
        return String.format("%s - %.2f%n", date.format(DateTimeConstants.DATE_TIME_FORMATTER), price);
    }
}
