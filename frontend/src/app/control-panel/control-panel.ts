import { Component, OnInit } from '@angular/core';
import { ShipmentService, Shipment } from '../services/shipment.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-control-panel',
  templateUrl: './control-panel.html',
  styleUrls: ['./control-panel.css'],
  imports: [FormsModule, CommonModule]
})
export class ControlPanelComponent implements OnInit {
  shipmentsList: Shipment[] = [];
  feedbackMessage: string = '';

  newShipment: Shipment = {
    trackingNumber: '',
    description: '',
    currentStatus: 'CREATED',
    customerUsername: ''
  };

  filterUsername: string = '';
  filterStatus: string = '';

  constructor(private shipmentService: ShipmentService) {}

  ngOnInit(): void {
    this.fetchShipments();
  }

  fetchShipments(): void {
    this.shipmentService.searchShipments(this.filterUsername, this.filterStatus).subscribe({
      next: (data) => {
        this.shipmentsList = data;
      },
      error: (err) => {
        this.feedbackMessage = 'Greška prilikom osvežavanja tabele.';
        console.error(err);
      }
    });
  }

  onCreateShipment(): void {
    if (!this.newShipment.trackingNumber || !this.newShipment.customerUsername) {
      this.feedbackMessage = 'Polja "Tracking Number" i "Korisnik" su obavezna!';
      return;
    }

    this.shipmentService.createShipment(this.newShipment).subscribe({
      next: (success) => {
        if (success) {
          this.feedbackMessage = 'Pošiljka uspešno evidentirana!';
          this.newShipment = { trackingNumber: '', description: '', currentStatus: 'CREATED', customerUsername: '' };
          this.fetchShipments();
        } else {
          this.feedbackMessage = 'Problem pri upisu. Proveri da li korisnik postoji.';
        }
      }
    });
  }

  onChangeStatus(id: number, status: string): void {
    this.shipmentService.updateStatus(id, status).subscribe({
      next: (success) => {
        if (success) {
          this.feedbackMessage = `Status paketa uspešno promenjen u [${status}]`;
          this.fetchShipments();
        }
      }
    });
  }

  onFilter(): void {
    this.fetchShipments();
  }

  onResetFilters(): void {
    this.filterUsername = '';
    this.filterStatus = '';
    this.fetchShipments();
  }
}