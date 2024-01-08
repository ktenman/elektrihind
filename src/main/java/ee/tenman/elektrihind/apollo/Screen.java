package ee.tenman.elektrihind.apollo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Screen {
    private Map<String, int[]> coordinates;
    private Map<String, Integer> seatCounts;
    private String id;
    private String name;

    public boolean isValidSeat(String rowAndSeat) {
        if (seatCounts == null) {
            throw new IllegalStateException("seatCounts must be initialized");
        }
        if (rowAndSeat != null && rowAndSeat.length() >= 4) {
            String row = rowAndSeat.substring(0, 2).toUpperCase(); // e.g., "S1" or "S2"
            Integer maxSeats = seatCounts.get(row);

            if (maxSeats != null) {
                return isValidSeatNumber(rowAndSeat.substring(3), maxSeats);
            }
        }
        return false;
    }

    private boolean isValidSeatNumber(String number, int maxSeats) {
        try {
            int seatNumber = Integer.parseInt(number);
            return seatNumber >= 1 && seatNumber <= maxSeats;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
