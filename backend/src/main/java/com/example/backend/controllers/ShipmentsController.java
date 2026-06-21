package com.example.backend.controllers;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.db.ShipmentsRepoInterface;
import com.example.backend.models.Shipment;

@RestController
@RequestMapping("/shipments")
@CrossOrigin(origins = "http://localhost:4200")
public class ShipmentsController {

    private static final Set<String> COURIER_STATUSES = Set.of("IN_TRANSIT", "DELIVERED");
    private static final Set<String> ALL_STATUSES = Set.of("CREATED", "IN_TRANSIT", "DELIVERED", "CANCELLED");

    private final ShipmentsRepoInterface shipmentsRepo;

    public ShipmentsController(ShipmentsRepoInterface shipmentsRepo) {
        this.shipmentsRepo = shipmentsRepo;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createShipment(
            @RequestBody Shipment shipment,
            @RequestParam String role,
            @RequestParam String requester) {
        // Only a "user" creates shipments, and always for themselves.
        if (!"user".equals(role) && !"admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only users can create shipments.");
        }

        shipment.setCustomerUsername(requester);
        shipment.setCurrentStatus("CREATED");

        boolean created = shipmentsRepo.createShipment(shipment);
        if (!created) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not create shipment.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(true);
    }

    @PostMapping("/update-status")
    public ResponseEntity<?> updateStatus(
            @RequestParam int id,
            @RequestParam String status,
            @RequestParam String role) {
        // Only couriers and admins can change a shipment status.
        if (!"courier".equals(role) && !"admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not allowed to change shipment status.");
        }

        if (!ALL_STATUSES.contains(status)) {
            return ResponseEntity.badRequest().body("Invalid status value.");
        }

        // A courier may only move shipments to IN_TRANSIT or DELIVERED.
        if ("courier".equals(role) && !COURIER_STATUSES.contains(status)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Couriers can only set IN_TRANSIT or DELIVERED.");
        }

        boolean updated = shipmentsRepo.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/search")
    public List<Shipment> searchShipments(
            @RequestParam String role,
            @RequestParam String requester,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date) {
        // A plain user can only ever see their own shipments.
        if ("user".equals(role)) {
            customer = requester;
        }
        return shipmentsRepo.searchShipments(customer, status, date);
    }
}