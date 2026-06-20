package com.example.backend.models;

import java.sql.Timestamp;

public class Shipment {
    private int id;
    private String trackingNumber;
    private String description;
    private String currentStatus;
    private String customerUsername;
    private Timestamp createdAt;

    public Shipment() {
    }

    public Shipment(int id, String trackingNumber, String description, String currentStatus, String customerUsername, Timestamp createdAt) {
        this.id = id;
        this.trackingNumber = trackingNumber;
        this.description = description;
        this.currentStatus = currentStatus;
        this.customerUsername = customerUsername;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getCustomerUsername() {
        return customerUsername;
    }

    public void setCustomerUsername(String customerUsername) {
        this.customerUsername = customerUsername;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}