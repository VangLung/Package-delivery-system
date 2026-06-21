package com.example.backend.db;

import java.util.List;
import com.example.backend.models.Shipment;
import com.example.backend.models.StatusLog;

public interface ShipmentsRepoInterface {
    public boolean createShipment(Shipment shipment);
    public boolean updateStatus(int shipmentId, String newStatus);
    public List<Shipment> searchShipments(String customer, String status, String date,
            boolean excludeDelivered, Integer cursor, int limit);
    public List<StatusLog> getHistory(int shipmentId);
    public int insertBatch(List<Shipment> batch);
}