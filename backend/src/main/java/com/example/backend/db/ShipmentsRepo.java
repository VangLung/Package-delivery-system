package com.example.backend.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import org.springframework.stereotype.Repository;
import com.example.backend.models.Shipment;

@Repository
public class ShipmentsRepo implements ShipmentsRepoInterface {

    private final DataSource dataSource;

    public ShipmentsRepo(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean createShipment(Shipment shipment) {
        String sql = "INSERT INTO shipments (tracking_number, description, current_status, customer_username) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, shipment.getTrackingNumber());
            stmt.setString(2, shipment.getDescription());
            stmt.setString(3, shipment.getCurrentStatus());
            stmt.setString(4, shipment.getCustomerUsername());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean updateStatus(int shipmentId, String newStatus) {
        String sql = "UPDATE shipments SET current_status = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, newStatus);
            stmt.setInt(2, shipmentId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Shipment> searchShipments(String customer, String status, String date) {
        List<Shipment> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM shipments WHERE 1=1");
        
        if (customer != null && !customer.isEmpty()) sql.append(" AND customer_username = ?");
        if (status != null && !status.isEmpty()) sql.append(" AND current_status = ?");
        if (date != null && !date.isEmpty()) sql.append(" AND DATE(created_at) = ?");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (customer != null && !customer.isEmpty()) stmt.setString(paramIndex++, customer);
            if (status != null && !status.isEmpty()) stmt.setString(paramIndex++, status);
            if (date != null && !date.isEmpty()) stmt.setString(paramIndex++, date);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Shipment(
                        rs.getInt("id"),
                        rs.getString("tracking_number"),
                        rs.getString("description"),
                        rs.getString("current_status"),
                        rs.getString("customer_username"),
                        rs.getTimestamp("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}