DROP TABLE IF EXISTS status_logs;
DROP TABLE IF EXISTS shipments;
DROP TABLE IF EXISTS users;


CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL -- 'admin', 'user' or 'courier'
);

CREATE TABLE shipments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tracking_number VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    current_status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    customer_username VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_username) REFERENCES users(username) ON DELETE SET NULL
);

CREATE TABLE status_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    shipment_id INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    note VARCHAR(255),
    FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE
);

DELIMITER $$

CREATE TRIGGER after_shipment_update
AFTER UPDATE ON shipments
FOR EACH ROW
BEGIN
    IF OLD.current_status <> NEW.current_status THEN
        INSERT INTO status_logs (shipment_id, status, note)
        VALUES (NEW.id, NEW.current_status, CONCAT('Status changed from ', OLD.current_status, ' to ', NEW.current_status, '.'));
    END IF;
END$$

DELIMITER ;