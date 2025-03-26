package org.example;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.util.*;

public class XMLFileHandler {

    /**
     * Parses an XML file into a list of Vehicle objects
     * @param file The XML file to parse
     * @return A list of Vehicle objects
     */
    public List<Vehicle> importXML(File file) {
        List<Vehicle> vehicles = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);
            document.getDocumentElement().normalize();

            NodeList dealerNodes = document.getElementsByTagName("Dealer");
            for (int i = 0; i < dealerNodes.getLength(); i++) {
                Element dealerElement = (Element) dealerNodes.item(i);
                String dealerId = dealerElement.getAttribute("id");

                // Try different name tags - support both "Name" and "n" tags
                String dealerName = getElementValue(dealerElement, "Name");
                if (dealerName.isEmpty()) {
                    dealerName = getElementValue(dealerElement, "n");
                }

                NodeList vehicleNodes = dealerElement.getElementsByTagName("Vehicle");
                for (int j = 0; j < vehicleNodes.getLength(); j++) {
                    Element vehicleElement = (Element) vehicleNodes.item(j);
                    Vehicle vehicle = createVehicleFromElement(vehicleElement, dealerId, dealerName);
                    if (vehicle != null) {
                        vehicles.add(vehicle);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing XML file: " + e.getMessage());
            // Still return whatever vehicles were successfully parsed
        }
        return vehicles;
    }

    /**
     * Creates a Vehicle object from an XML element
     */
    private Vehicle createVehicleFromElement(Element vehicleElement, String dealerId, String dealerName) {
        try {
            // Get vehicle type with default if missing
            String vehicleType = "suv"; // Default type
            if (vehicleElement.hasAttribute("type")) {
                vehicleType = vehicleElement.getAttribute("type").toLowerCase();
            }

            // Get vehicle ID with generated ID if missing
            String vehicleId = "";
            if (vehicleElement.hasAttribute("id")) {
                vehicleId = vehicleElement.getAttribute("id");
            } else {
                vehicleId = generateRandomId();
            }

            // Get make and model
            String make = getElementValue(vehicleElement, "Make");
            if (make.isEmpty()) {
                // Try alternate tag name "Manufacturer"
                make = getElementValue(vehicleElement, "Manufacturer");
            }

            String model = getElementValue(vehicleElement, "Model");

            // Initialize price with default
            double price = 0.0;

            // Get price element safely
            NodeList priceNodes = vehicleElement.getElementsByTagName("Price");
            if (priceNodes.getLength() > 0) {
                Element priceElement = (Element) priceNodes.item(0);

                try {
                    // Default to dollars if unit isn't specified
                    String priceUnit = "dollars";
                    if (priceElement.hasAttribute("unit")) {
                        priceUnit = priceElement.getAttribute("unit");
                    }

                    price = Double.parseDouble(priceElement.getTextContent());

                    // Convert pounds to dollars
                    if ("pounds".equals(priceUnit)) {
                        price = price * 1.25; // Example conversion rate
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid price format: " + e.getMessage());
                    // Keep default price of 0.0
                }
            }

            // Create the appropriate vehicle type
            Vehicle vehicle;
            switch (vehicleType) {
                case "suv":
                    vehicle = new SUV();
                    break;
                case "sedan":
                    vehicle = new Sedan();
                    break;
                case "pickup":
                    vehicle = new Pickup();
                    break;
                case "sports car":
                    vehicle = new SportsCar();
                    break;
                default:
                    // Default to SUV for unknown types
                    vehicle = new SUV();
            }

            // Set all vehicle properties
            vehicle.setVehicleId(vehicleId);
            vehicle.setManufacturer(make);
            vehicle.setModel(model);
            vehicle.setPrice(price);
            vehicle.setDealerId(dealerId);
            vehicle.setAcquisitionDate(new Date());

            // Store dealer name in metadata (even if empty)
            vehicle.getMetadata().put("dealer_name", dealerName != null ? dealerName : "");

            return vehicle;
        } catch (Exception e) {
            System.err.println("Error creating vehicle: " + e.getMessage());
            return null;
        }
    }

    /**
     * Helper method to get element value
     */
    private String getElementValue(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }

    /**
     * Generates a random ID for vehicles without an ID attribute
     */
    private String generateRandomId() {
        return "GEN-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
}