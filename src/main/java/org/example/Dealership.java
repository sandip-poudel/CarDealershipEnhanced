package org.example;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * The Dealership class is a car dealership that manages an inventory of vehicle.
 */
public class Dealership {
    private String dealerId;                            // Unique id for dealership
    private boolean isAcquisitionEnabled = true;        // Controls whether vehicle acquisition is allowed
    private List<Vehicle> vehicles = new ArrayList<>(); // List that stores vehicle in the dealership
    private String name;                                // Dealership name for display

    /**
     * Constructor that initialize a Dealership
     * @param dealerId Unique id for dealership
     */
    public Dealership(String dealerId) {
        this.dealerId = dealerId;
    }

    /**
     * Constructor with dealer name
     * @param dealerId Unique id for dealership
     * @param name Name of the dealership
     */
    public Dealership(String dealerId, String name) {
        this.dealerId = dealerId;
        this.name = name;
    }

    /**
     * Enables vehicle acquisition
     */
    public void enableAcquisition() {
        isAcquisitionEnabled = true;
    }

    /**
     * Disables vehicle acquisition
     */
    public void disableAcquisition() {
        isAcquisitionEnabled = false;
    }

    /**
     * Checks if the acquisition is enabled or not
     * @return true if acquisition is enabled, otherwise false
     */
    public boolean isAcquisitionEnabled() {
        return isAcquisitionEnabled;
    }

    /**
     * Adds a vehicle to the inventory if it doesn't exist
     * @param vehicle The vehicle you want to add
     * @return true if vehicle was added, false otherwise
     */
    public boolean addVehicle(Vehicle vehicle) {
        // Check if vehicle with same ID already exists
        if (vehicles.stream().anyMatch(v -> v.getVehicleId().equals(vehicle.getVehicleId()))) {
            return false;
        }
        // Add the new vehicle
        vehicles.add(vehicle);
        return true;
    }

    /**
     * Exports the inventory to a JSON file
     * @param file The file that the inventory is going to be written into
     */
    public void exportToJSON(File file) {
        JSONFileHandler handler = new JSONFileHandler();
        handler.writeInventory(vehicles, file);
    }

    /**
     * Gets a copy of the vehicle inventory
     * @return A list of vehicles in the inventory
     */
    public List<Vehicle> getVehicles() {
        return new ArrayList<>(vehicles);
    }

    /**
     * Gets dealer id
     * @return dealerId
     */
    public String getDealerId() {
        return dealerId;
    }

    /**
     * Gets dealership name
     * @return name of dealership
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the dealership name
     * @param name the new name for the dealership
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Transfers a vehicle to another dealership
     * @param vehicleId The ID of the vehicle to transfer
     * @param targetDealership The dealership to transfer to
     * @return true if transfer was successful, false otherwise
     */
    public boolean transferVehicle(String vehicleId, Dealership targetDealership) {
        Vehicle vehicleToTransfer = null;
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getVehicleId().equals(vehicleId)) {
                vehicleToTransfer = vehicle;
                break;
            }
        }

        if (vehicleToTransfer == null) return false;

        // Can't transfer a rented vehicle
        if (vehicleToTransfer.isRented()) return false;

        // Remove from this dealership
        vehicles.remove(vehicleToTransfer);

        // Update vehicle's dealerId
        vehicleToTransfer.setDealerId(targetDealership.getDealerId());

        // Add to target dealership
        return targetDealership.addVehicle(vehicleToTransfer);
    }

    /**
     * Finds a vehicle by ID
     * @param vehicleId The ID to search for
     * @return The found vehicle or null
     */
    public Vehicle findVehicleById(String vehicleId) {
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getVehicleId().equals(vehicleId)) {
                return vehicle;
            }
        }
        return null;
    }

    /**
     * Rents a vehicle by ID
     * @param vehicleId The ID of the vehicle to rent
     * @param startDate The rental start date
     * @param endDate The rental end date
     * @return true if successful, false otherwise
     */
    public boolean rentVehicle(String vehicleId, Date startDate, Date endDate) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle == null) return false;
        return vehicle.rent(startDate, endDate);
    }

    /**
     * Returns a rented vehicle
     * @param vehicleId The ID of the vehicle to return
     * @return true if successful, false otherwise
     */
    public boolean returnVehicle(String vehicleId) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle == null) return false;
        return vehicle.returnVehicle();
    }

    /**
     * Displays the details of all the vehicles in the dealership
     */
    public void showVehicles() {
        System.out.println("\nDealership ID: " + dealerId);
        if (name != null && !name.isEmpty()) {
            System.out.println("Dealership Name: " + name);
        }
        System.out.println("Total vehicles: " + vehicles.size());
        vehicles.forEach(v -> System.out.println(
                "Type: " + v.getClass().getSimpleName() +
                        ", ID: " + v.getVehicleId() +
                        ", Manufacturer: " + v.getManufacturer() +
                        ", Model: " + v.getModel() +
                        ", Price: $" + v.getPrice() +
                        ", Status: " + (v.isRented() ? "RENTED" : "AVAILABLE")
        ));
    }
}
