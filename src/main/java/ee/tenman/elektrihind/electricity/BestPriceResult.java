package ee.tenman.elektrihind.electricity;

public class BestPriceResult {
    private String startTime;
    private Double totalCost;

    public BestPriceResult(String startTime, Double totalCost) {
        this.startTime = startTime;
        this.totalCost = totalCost;
    }

    public String getStartTime() {
        return startTime;
    }

    public Double getTotalCost() {
        return totalCost;
    }
}
