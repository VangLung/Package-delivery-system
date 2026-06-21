package com.example.backend.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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

    private static final String LOG_SQL =
        "INSERT INTO status_logs (shipment_id, status, note) VALUES (?, ?, ?)";
    private static final String CREATED_NOTE = "Shipment successfully created in the system.";

    @Override
    public boolean createShipment(Shipment shipment) {
        String sql = "INSERT INTO shipments (tracking_number, description, current_status, customer_username) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, shipment.getTrackingNumber());
            stmt.setString(2, shipment.getDescription());
            stmt.setString(3, shipment.getCurrentStatus());
            stmt.setString(4, shipment.getCustomerUsername());

            if (stmt.executeUpdate() == 0) {
                return false;
            }
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    try (PreparedStatement logStmt = conn.prepareStatement(LOG_SQL)) {
                        logStmt.setLong(1, id);
                        logStmt.setString(2, shipment.getCurrentStatus());
                        logStmt.setString(3, CREATED_NOTE);
                        logStmt.executeUpdate();
                    }
                }
            }
            return true;
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

    @Override
    public int insertBatch(List<Shipment> batch) {
        String sql = "INSERT INTO shipments (tracking_number, description, current_status, customer_username, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (Shipment s : batch) {
                    stmt.setString(1, s.getTrackingNumber());
                    stmt.setString(2, s.getDescription());
                    stmt.setString(3, s.getCurrentStatus());
                    stmt.setString(4, s.getCustomerUsername());
                    stmt.setTimestamp(5, s.getCreatedAt() != null ? s.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
                    stmt.addBatch();
                }
                stmt.executeBatch();

                // The insert trigger was removed for speed, so we write the
                // initial status history here, in bulk, using the generated ids.
                List<Long> ids = new ArrayList<>(batch.size());
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    while (keys.next()) {
                        ids.add(keys.getLong(1));
                    }
                }
                try (PreparedStatement logStmt = conn.prepareStatement(LOG_SQL)) {
                    for (int i = 0; i < ids.size(); i++) {
                        logStmt.setLong(1, ids.get(i));
                        logStmt.setString(2, batch.get(i).getCurrentStatus());
                        logStmt.setString(3, CREATED_NOTE);
                        logStmt.addBatch();
                    }
                    logStmt.executeBatch();
                }

                conn.commit();
                return batch.size();
            } catch (SQLException e) {
                conn.rollback();
                return 0;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return 0;
        }
    }
}