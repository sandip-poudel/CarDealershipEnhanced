package org.example;

public class DealerStats {
    private final String dealerId;
    private final int vehicleCount;

    public DealerStats(String dealerId, int vehicleCount) {
        this.dealerId = dealerId;
        this.vehicleCount = vehicleCount;
    }

    public String getDealerId() {
        return dealerId;
    }

    public int getVehicleCount() {
        return vehicleCount;
    }
}