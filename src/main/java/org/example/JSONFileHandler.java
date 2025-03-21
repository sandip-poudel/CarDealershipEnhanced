package org.example;

import com.fasterxml.jackson.databind.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class JSONFileHandler {
    private final ObjectMapper objectMapper;

    public JSONFileHandler() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<Vehicle> readInventory(File file) {
        try {
            if (!file.exists()) {
                return new ArrayList<>();
            }
            JsonNode rootNode = objectMapper.readTree(file);
            JsonNode inventory = rootNode.get("car_inventory");
            if (inventory == null) return Collections.emptyList();

            List<Vehicle> vehicles = new ArrayList<>();
            for (JsonNode node : inventory) {
                Vehicle vehicle = inferVehicleType(node);
                if (vehicle != null) {
                    vehicles.add(vehicle);
                }
            }
            return vehicles;
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private Vehicle inferVehicleType(JsonNode node) {
        try {
            String model = node.get("vehicle_model").asText().toLowerCase();
            Vehicle vehicle;

            if (model.contains("cr-v") || model.contains("explorer") || model.contains("range rover")) {
                vehicle = new SUV();
            } else if (model.contains("model 3") || model.contains("g70")) {
                vehicle = new Sedan();
            } else if (model.contains("silverado") || model.contains("tundra")) {
                vehicle = new Pickup();
            } else if (model.contains("supra") || model.contains("miata")) {
                vehicle = new SportsCar();
            } else {
                vehicle = new SUV();
            }

            vehicle.setVehicleId(node.get("vehicle_id").asText());
            vehicle.setManufacturer(node.get("vehicle_manufacturer").asText());
            vehicle.setModel(node.get("vehicle_model").asText());
            vehicle.setPrice(node.get("price").asDouble());
            vehicle.setDealerId(node.get("dealership_id").asText());
            vehicle.setAcquisitionDate(new Date(node.get("acquisition_date").asLong()));

            // Handle rental information
            if (node.has("is_rented")) {
                vehicle.setRented(node.get("is_rented").asBoolean());
            }
            if (node.has("rental_start_date")) {
                vehicle.setRentalStartDate(new Date(node.get("rental_start_date").asLong()));
            }
            if (node.has("rental_end_date")) {
                vehicle.setRentalEndDate(new Date(node.get("rental_end_date").asLong()));
            }

            // Handle dealer name in metadata
            if (node.has("dealer_name")) {
                vehicle.getMetadata().put("dealer_name", node.get("dealer_name").asText());
            }

            return vehicle;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void writeInventory(List<Vehicle> vehicles, File file) {
        try {
            Map<String, Map<String, Object>> vehicleMap = new HashMap<>();

            for (Vehicle vehicle : vehicles) {
                Map<String, Object> vehicleData = new HashMap<>();
                vehicleData.put("vehicle_id", vehicle.getVehicleId());
                vehicleData.put("vehicle_manufacturer", vehicle.getManufacturer());
                vehicleData.put("vehicle_model", vehicle.getModel());
                vehicleData.put("acquisition_date", vehicle.getAcquisitionDate().getTime());
                vehicleData.put("price", vehicle.getPrice());
                vehicleData.put("dealership_id", vehicle.getDealerId());
                vehicleData.put("vehicle_type", getVehicleType(vehicle));

                // Add rental information
                vehicleData.put("is_rented", vehicle.isRented());
                if (vehicle.getRentalStartDate() != null) {
                    vehicleData.put("rental_start_date", vehicle.getRentalStartDate().getTime());
                }
                if (vehicle.getRentalEndDate() != null) {
                    vehicleData.put("rental_end_date", vehicle.getRentalEndDate().getTime());
                }

                // Add dealer name if available
                if (vehicle.getMetadata().containsKey("dealer_name")) {
                    vehicleData.put("dealer_name", vehicle.getMetadata().get("dealer_name"));
                }

                vehicleMap.put(vehicle.getVehicleId(), vehicleData);
            }

            Map<String, List<Map<String, Object>>> wrapper = new HashMap<>();
            wrapper.put("car_inventory", new ArrayList<>(vehicleMap.values()));
            objectMapper.writeValue(file, wrapper);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getVehicleType(Vehicle vehicle) {
        if (vehicle instanceof SUV) return "suv";
        if (vehicle instanceof Sedan) return "sedan";
        if (vehicle instanceof Pickup) return "pickup";
        if (vehicle instanceof SportsCar) return "sports car";
        return "unknown";
    }
}
