package ee.tenman.elektrihind.apollo;

import java.util.Map;

public record Screen(
        Map<String, int[]> coordinates,
        Map<String, Integer> seatCounts,
        String id,
        String name) {
}
