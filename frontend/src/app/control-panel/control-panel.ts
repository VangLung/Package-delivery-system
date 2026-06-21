import { Component, OnInit, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ShipmentService, Shipment } from '../services/shipment.service';
import { AuthService } from '../services/auth.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-control-panel',
  templateUrl: './control-panel.html',
  styleUrls: ['./control-panel.css'],
  imports: [FormsModule, CommonModule]
})
export class ControlPanelComponent implements OnInit {
  private shipmentService = inject(ShipmentService);
  private authService = inject(AuthService);
  private router = inject(Router);

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
  filterDate: string = '';

  readonly currentUser = this.authService.currentUser;
  readonly canCreate = computed(() => {
    const role = this.authService.role();
    return role === 'user' || role === 'admin';
  });
  readonly canChangeStatus = computed(() => {
    const role = this.authService.role();
    return role === 'courier' || role === 'admin';
  });
  readonly canFilterByUser = computed(() => {
    const role = this.authService.role();
    return role === 'courier' || role === 'admin';
  });

  ngOnInit(): void {
    this.fetchShipments();
  }

  fetchShipments(): void {
    this.shipmentService.searchShipments(this.filterUsername, this.filterStatus, this.filterDate).subscribe({
      next: (data) => {
        this.shipmentsList = data;
      },
      error: (err) => {
        this.feedbackMessage = 'Error refreshing table data.';
        console.error(err);
      }
    });
  }

  onCreateShipment(): void {
    if (!this.newShipment.trackingNumber) {
      this.feedbackMessage = 'Tracking Number is required!';
      return;
    }

    this.shipmentService.createShipment(this.newShipment).subscribe({
      next: (success) => {
        if (success) {
          this.feedbackMessage = 'Shipment successfully created!';
          this.newShipment = { trackingNumber: '', description: '', currentStatus: 'CREATED', customerUsername: '' };
          this.fetchShipments();
        } else {
          this.feedbackMessage = 'Insertion failed. Verify the input data.';
        }
      },
      error: () => {
        this.feedbackMessage = 'Insertion failed. Verify the input data.';
      }
    });
  }

  onChangeStatus(id: number, status: string): void {
    this.shipmentService.updateStatus(id, status).subscribe({
      next: (success) => {
        if (success) {
          this.feedbackMessage = `Shipment status updated to [${status}]`;
          this.fetchShipments();
        }
      },
      error: () => {
        this.feedbackMessage = 'You are not allowed to perform this action.';
      }
    });
  }

  onFilter(): void {
    this.fetchShipments();
  }

  onResetFilters(): void {
    this.filterUsername = '';
    this.filterStatus = '';
    this.filterDate = '';
    this.fetchShipments();
  }

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}