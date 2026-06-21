import { Component, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ShipmentService, Shipment } from '../services/shipment.service';
import { ShipmentListComponent } from '../shipment-list/shipment-list';

@Component({
  selector: 'app-user-panel',
  imports: [FormsModule, ShipmentListComponent],
  templateUrl: './user-panel.html'
})
export class UserPanelComponent {
  private shipmentService = inject(ShipmentService);

  @ViewChild(ShipmentListComponent) list!: ShipmentListComponent;

  readonly message = signal('');
  newShipment: Shipment = {
    trackingNumber: '',
    description: '',
    currentStatus: 'CREATED',
    customerUsername: ''
  };

  create(): void {
    if (!this.newShipment.trackingNumber) {
      this.message.set('Tracking Number is required.');
      return;
    }
    this.shipmentService.createShipment(this.newShipment).subscribe({
      next: (ok) => {
        if (ok) {
          this.message.set('Shipment created.');
          this.newShipment = { trackingNumber: '', description: '', currentStatus: 'CREATED', customerUsername: '' };
          this.list.reload();
        } else {
          this.message.set('Creation failed. Verify the input data.');
        }
      },
      error: () => this.message.set('Creation failed. Verify the input data.')
    });
  }
}
