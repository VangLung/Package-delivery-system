package com.example.backend.db;

import java.util.List;
import com.example.backend.models.Shipment;

public interface ShipmentsRepoInterface {
    public boolean createShipment(Shipment shipment);
    public boolean updateStatus(int shipmentId, String newStatus);
    public List<Shipment> searchShipments(String customer, String status, String date);
    public int insertBatch(List<Shipment> batch);
}