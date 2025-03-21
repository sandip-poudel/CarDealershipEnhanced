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
                String dealerName = getElementValue(dealerElement, "Name");

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
            e.printStackTrace();
        }
        return vehicles;
    }

    /**
     * Creates a Vehicle object from an XML element
     */
    private Vehicle createVehicleFromElement(Element vehicleElement, String dealerId, String dealerName) {
        try {
            String vehicleType = vehicleElement.getAttribute("type").toLowerCase();
            String vehicleId = vehicleElement.getAttribute("id");
            String make = getElementValue(vehicleElement, "Make");
            String model = getElementValue(vehicleElement, "Model");

            Element priceElement = (Element) vehicleElement.getElementsByTagName("Price").item(0);
            String priceUnit = priceElement.getAttribute("unit");
            double price = Double.parseDouble(priceElement.getTextContent());

            // Convert pounds to dollars if needed
            if ("pounds".equals(priceUnit)) {
                price = price * 1.25; // Example conversion rate
            }

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
                    vehicle = new SUV(); // Default
            }

            vehicle.setVehicleId(vehicleId);
            vehicle.setManufacturer(make);
            vehicle.setModel(model);
            vehicle.setPrice(price);
            vehicle.setDealerId(dealerId);
            vehicle.setAcquisitionDate(new Date());

            // Store dealer name in metadata
            vehicle.getMetadata().put("dealer_name", dealerName);

            return vehicle;
        } catch (Exception e) {
            e.printStackTrace();
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
}
