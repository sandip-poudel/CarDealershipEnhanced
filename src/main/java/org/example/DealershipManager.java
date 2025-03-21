package org.example;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The DealershipManager class manages all the dealerships and the inventories.
 * This class provides the methods add, remove, export vehicle, enable acquisition, and lastly disable acquisition.
 */
public class DealershipManager {
    private Map<String, Dealership> dealerships = new HashMap<>();          // stores the dealership by their id
    private final JSONFileHandler jsonFileHandler = new JSONFileHandler();  // Handles all the JSON files
    private final XMLFileHandler xmlFileHandler = new XMLFileHandler();     // Handles XML import

    /**
     * Reads the inventory and loads the vehicles into their respective dealership
     * @param file The inventory file that you want to read form
     */
    public void readInventoryFile(File file) {
        List<Vehicle> vehicles = jsonFileHandler.readInventory(file);
        for (Vehicle vehicle : vehicles) {
            String dealerId = vehicle.getDealerId();
            String dealerName = null;

            // Check if dealer name is in metadata
            if (vehicle.getMetadata().containsKey("dealer_name")) {
                dealerName = (String) vehicle.getMetadata().get("dealer_name");
            }

            processAddVehicleCommand(dealerId, vehicle, dealerName);
        }
    }

    /**
     * A command to add a vehicle to a dealership
     * @param dealerId  Unique id for dealership
     * @param vehicle   The vehicle you want added
     * @return  true if the vehicle was added, false otherwise
     */
    public boolean processAddVehicleCommand(String dealerId, Vehicle vehicle) {
        return processAddVehicleCommand(dealerId, vehicle, null);
    }

    /**
     * A command to add a vehicle to a dealership with dealer name
     * @param dealerId Unique id for dealership
     * @param vehicle The vehicle you want added
     * @param dealerName Optional dealer name
     * @return true if the vehicle was added, false otherwise
     */
    public boolean processAddVehicleCommand(String dealerId, Vehicle vehicle, String dealerName) {
        Dealership dealership = dealerships.get(dealerId);

        if (dealership == null) {
            dealership = new Dealership(dealerId, dealerName);
            dealership.enableAcquisition();
            dealerships.put(dealerId, dealership);
        } else if (dealerName != null && !dealerName.isEmpty()) {
            dealership.setName(dealerName);
        }

        if (!dealership.isAcquisitionEnabled()) {
            System.out.println("Cannot add vehicle: Acquisition disabled for dealer " + dealerId);
            return false;
        }

        if (dealership.addVehicle(vehicle)) {
            System.out.println("Vehicle added successfully to dealer " + dealerId);
            return true;
        } else {
            System.out.println("Failed to add vehicle: Duplicate vehicle ID");
            return false;
        }
    }

    /**
     * Adds a vehicle to the inventory as well as updating the dealership
     * @param vehicle The vehicle you want added
     * @param inventoryFile The file where the inventory is stored
     * @return true if the vehicle was added, false otherwise
     */
    public boolean addVehicleToInventory(Vehicle vehicle, File inventoryFile) {
        // Check if acquisition is enabled
        Dealership dealership = dealerships.get(vehicle.getDealerId());
        if (dealership != null && !dealership.isAcquisitionEnabled()) {
            return false;
        }

        boolean result = processAddVehicleCommand(vehicle.getDealerId(), vehicle);
        if (result) {
            saveState(inventoryFile);
        }
        return result;
    }

    /**
     * Auto-saves the current state to the inventory file
     * @param inventoryFile The file to save to
     */
    private void saveState(File inventoryFile) {
        List<Vehicle> allVehicles = getVehiclesForDisplay();
        jsonFileHandler.writeInventory(allVehicles, inventoryFile);
    }

    /**
     * Removes a vehicle from inventory and dealership lists.
     * @param dealerId The unique id of a dealership
     * @param vehicleId The id of the vehicle you want removed
     * @param manufacturer  The manufacturer of the vehicle
     * @param model The model of the vehicle
     * @param price The price of the vehicle
     * @param inventoryFile The file where the inventory is stored
     * @return  true if the vehicle was removed, otherwise false
     */
    public boolean removeVehicleFromInventory(String dealerId, String vehicleId, String manufacturer,
                                              String model, double price, File inventoryFile) {
        // Find the dealership
        Dealership dealership = dealerships.get(dealerId);
        if (dealership == null) {
            return false;
        }

        // Find the vehicle in the dealership
        Vehicle vehicleToRemove = null;
        for (Vehicle vehicle : dealership.getVehicles()) {
            if (vehicle.getVehicleId().equals(vehicleId) &&
                    vehicle.getManufacturer().equals(manufacturer) &&
                    vehicle.getModel().equals(model) &&
                    Math.abs(vehicle.getPrice() - price) < 0.01) {
                vehicleToRemove = vehicle;
                break;
            }
        }

        if (vehicleToRemove == null) {
            return false;
        }

        // Can't remove a rented vehicle
        if (vehicleToRemove.isRented()) {
            return false;
        }

        // Remove vehicle from dealership list
        List<Vehicle> vehicles = new ArrayList<>(dealership.getVehicles());
        vehicles.remove(vehicleToRemove);
        dealerships.put(dealerId, dealership);

        // Save updated state
        saveState(inventoryFile);
        return true;
    }

    /**
     * Exports the inventory to an external file
     * @param inventoryFile The inventory file
     * @param exportFile The destination export file
     * @return true if export is successful, otherwise false
     */
    public boolean exportInventoryToExport(File inventoryFile, File exportFile) {
        List<Vehicle> inventory = jsonFileHandler.readInventory(inventoryFile);
        if (inventory.isEmpty()) {
            System.out.println("No vehicles to export.");
            return false;
        }

        try {
            jsonFileHandler.writeInventory(inventory, exportFile);
            System.out.println("Exported " + inventory.size() + " vehicles to export.json");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Clears the export file by writing an empty inventory list
     * @param exportFile The file you want cleared
     */
    public void clearExportFile(File exportFile) {
        List<Vehicle> emptyList = new ArrayList<>();
        jsonFileHandler.writeInventory(emptyList, exportFile);
    }

    /**
     * Enables vehicle acquisition for a specific dealership
     * @param dealerId The unique id of the dealership
     * @return true after acquisition was enabled
     */
    public boolean enableAcquisition(String dealerId) {
        Dealership dealership = dealerships.get(dealerId);
        if (dealership == null) {
            dealership = new Dealership(dealerId);
            dealerships.put(dealerId, dealership);
        }
        dealership.enableAcquisition();
        return true;
    }

    /**
     * Disables vehicle acquisition for a specific dealership
     * @param dealerId The unique id of the dealership
     * @return true after disabling acquisition
     */
    public boolean disableAcquisition(String dealerId) {
        Dealership dealership = dealerships.get(dealerId);
        if (dealership == null) {
            dealership = new Dealership(dealerId);
            dealerships.put(dealerId, dealership);
        }
        dealership.disableAcquisition();
        return true;
    }

    /**
     * Imports vehicles from an XML file
     * @param xmlFile The XML file to import
     * @param inventoryFile The inventory file to update
     * @return Number of vehicles successfully imported
     */
    public int importXMLFile(File xmlFile, File inventoryFile) {
        List<Vehicle> importedVehicles = xmlFileHandler.importXML(xmlFile);
        int successCount = 0;

        for (Vehicle vehicle : importedVehicles) {
            String dealerId = vehicle.getDealerId();
            String dealerName = null;

            if (vehicle.getMetadata().containsKey("dealer_name")) {
                dealerName = (String) vehicle.getMetadata().get("dealer_name");
            }

            if (processAddVehicleCommand(dealerId, vehicle, dealerName)) {
                successCount++;
            }
        }

        if (successCount > 0) {
            saveState(inventoryFile);
        }

        return successCount;
    }

    /**
     * Transfers a vehicle from one dealership to another
     * @param sourceDealerId The ID of the source dealership
     * @param targetDealerId The ID of the target dealership
     * @param vehicleId The ID of the vehicle to transfer
     * @param inventoryFile The inventory file to update
     * @return true if transfer was successful, false otherwise
     */
    public boolean transferVehicle(String sourceDealerId, String targetDealerId, String vehicleId, File inventoryFile) {
        Dealership sourceDealership = dealerships.get(sourceDealerId);
        Dealership targetDealership = dealerships.get(targetDealerId);

        if (sourceDealership == null || targetDealership == null) return false;
        if (!targetDealership.isAcquisitionEnabled()) return false;

        boolean result = sourceDealership.transferVehicle(vehicleId, targetDealership);
        if (result) {
            saveState(inventoryFile);
        }
        return result;
    }

    /**
     * Rents a vehicle
     * @param dealerId The dealer ID
     * @param vehicleId The vehicle ID
     * @param startDateStr The rental start date string (MM/dd/yyyy)
     * @param endDateStr The rental end date string (MM/dd/yyyy)
     * @param inventoryFile The inventory file to update
     * @return true if successful, false otherwise
     */
    public boolean rentVehicle(String dealerId, String vehicleId, String startDateStr, String endDateStr, File inventoryFile) {
        try {
            Dealership dealership = dealerships.get(dealerId);
            if (dealership == null) return false;

            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            Date startDate = dateFormat.parse(startDateStr);
            Date endDate = dateFormat.parse(endDateStr);

            boolean result = dealership.rentVehicle(vehicleId, startDate, endDate);
            if (result) {
                saveState(inventoryFile);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns a rented vehicle
     * @param dealerId The dealer ID
     * @param vehicleId The vehicle ID
     * @param inventoryFile The inventory file to update
     * @return true if successful, false otherwise
     */
    public boolean returnVehicle(String dealerId, String vehicleId, File inventoryFile) {
        Dealership dealership = dealerships.get(dealerId);
        if (dealership == null) return false;

        boolean result = dealership.returnVehicle(vehicleId);
        if (result) {
            saveState(inventoryFile);
        }
        return result;
    }

    /**
     * Gets a list of all vehicles for all the dealerships
     * @return A list containing all vehicles in all the dealerships
     */
    public List<Vehicle> getVehiclesForDisplay() {
        List<Vehicle> allVehicles = new ArrayList<>();
        for (Dealership dealership : dealerships.values()) {
            allVehicles.addAll(dealership.getVehicles());
        }
        return allVehicles;
    }
}
