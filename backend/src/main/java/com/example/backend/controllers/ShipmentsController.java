package com.example.backend.controllers;

import java.util.List;
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

    private final ShipmentsRepoInterface shipmentsRepo;

    public ShipmentsController(ShipmentsRepoInterface shipmentsRepo) {
        this.shipmentsRepo = shipmentsRepo;
    }

    @PostMapping("/create")
    public boolean createShipment(@RequestBody Shipment shipment) {
        return shipmentsRepo.createShipment(shipment);
    }

    @PostMapping("/update-status")
    public boolean updateStatus(@RequestParam int id, @RequestParam String status) {
        return shipmentsRepo.updateStatus(id, status);
    }

    @GetMapping("/search")
    public List<Shipment> searchShipments(
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date) {
        return shipmentsRepo.searchShipments(customer, status, date);
    }
}