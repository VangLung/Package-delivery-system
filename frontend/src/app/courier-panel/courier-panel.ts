import { Component } from '@angular/core';
import { ShipmentListComponent } from '../shipment-list/shipment-list';

@Component({
  selector: 'app-courier-panel',
  imports: [ShipmentListComponent],
  template: `
    <app-shipment-list
      title="Active Shipments (not delivered)"
      [canChangeStatus]="true"
      [excludeDelivered]="true">
    </app-shipment-list>
  `
})
export class CourierPanelComponent {}
