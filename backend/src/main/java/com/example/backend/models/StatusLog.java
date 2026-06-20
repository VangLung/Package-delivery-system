package com.example.backend.models;

import java.sql.Timestamp;

public class StatusLog {
    private int id;
    private int shipmentId;
    private String status;
    private Timestamp changedAt;
    private String note;

    public StatusLog() {
    }

    public StatusLog(int id, int shipmentId, String status, Timestamp changedAt, String note) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.status = status;
        this.changedAt = changedAt;
        this.note = note;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(int shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Timestamp changedAt) {
        this.changedAt = changedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}