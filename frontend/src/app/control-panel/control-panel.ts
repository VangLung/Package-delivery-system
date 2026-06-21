import { Component, OnInit, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ShipmentService, Shipment, ImportJob } from '../services/shipment.service';
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

  selectedFile: File | null = null;
  importing = false;
  importJob: ImportJob | null = null;

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
  readonly canImport = computed(() => this.authService.role() === 'admin');

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

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files && input.files.length > 0 ? input.files[0] : null;
  }

  onUpload(): void {
    if (!this.selectedFile) {
      this.feedbackMessage = 'Please choose a CSV file first.';
      return;
    }
    this.importing = true;
    this.importJob = null;
    this.shipmentService.importCsv(this.selectedFile).subscribe({
      next: (jobId) => this.pollImport(jobId),
      error: () => {
        this.importing = false;
        this.feedbackMessage = 'Import failed to start.';
      }
    });
  }

  private pollImport(jobId: string): void {
    this.shipmentService.getImportStatus(jobId).subscribe({
      next: (job) => {
        this.importJob = job;
        if (job.status === 'RUNNING') {
          setTimeout(() => this.pollImport(jobId), 1000);
        } else {
          this.importing = false;
          this.selectedFile = null;
          this.fetchShipments();
        }
      },
      error: () => {
        this.importing = false;
      }
    });
  }
}