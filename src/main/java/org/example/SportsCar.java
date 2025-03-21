package org.example;

public class SportsCar extends Vehicle {
    @Override
    public boolean isAvailableForRent() {
        return false; // Sports cars cannot be rented
    }
}
