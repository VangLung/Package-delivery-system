package com.example.backend.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.db.ShipmentsRepoInterface;
import com.example.backend.models.Shipment;
import com.example.backend.models.StatusLog;
import com.example.backend.service.ImportJob;
import com.example.backend.service.ImportService;

@RestController
@RequestMapping("/shipments")
@CrossOrigin(origins = "http://localhost:4200")
public class ShipmentsController {

    private static final Set<String> COURIER_STATUSES = Set.of("IN_TRANSIT", "DELIVERED");
    private static final Set<String> ALL_STATUSES = Set.of("CREATED", "IN_TRANSIT", "DELIVERED", "CANCELLED");

    private final ShipmentsRepoInterface shipmentsRepo;
    private final ImportService importService;

    public ShipmentsController(ShipmentsRepoInterface shipmentsRepo, ImportService importService) {
        this.shipmentsRepo = shipmentsRepo;
        this.importService = importService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createShipment(
            @RequestBody Shipment shipment,
            @RequestAttribute("role") String role,
            @RequestAttribute("username") String requester) {
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
            @RequestAttribute("role") String role) {
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
            @RequestAttribute("role") String role,
            @RequestAttribute("username") String requester,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer cursor,
            @RequestParam(defaultValue = "50") int limit) {
        boolean excludeDelivered = false;
        if ("user".equals(role)) {
            customer = requester;
        } else if ("courier".equals(role)) {
            excludeDelivered = true;
        }
        return shipmentsRepo.searchShipments(customer, status, date, excludeDelivered, cursor, limit);
    }

    @GetMapping("/{id}/history")
    public List<StatusLog> history(@PathVariable int id) {
        return shipmentsRepo.getHistory(id);
    }

    @PostMapping("/import")
    public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file, @RequestAttribute("role") String role)
            throws IOException {
        if (!"admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can import shipments.");
        }
        return ResponseEntity.accepted().body(importService.start(file));
    }

    @GetMapping("/import/{jobId}")
    public ResponseEntity<?> importStatus(@PathVariable String jobId) {
        ImportJob job = importService.job(jobId);
        return job == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(job);
    }
}