import { Component, ViewChild, inject, signal } from '@angular/core';
import { ShipmentService, ImportJob } from '../services/shipment.service';
import { ShipmentListComponent } from '../shipment-list/shipment-list';

@Component({
  selector: 'app-admin-panel',
  imports: [ShipmentListComponent],
  templateUrl: './admin-panel.html'
})
export class AdminPanelComponent {
  private shipmentService = inject(ShipmentService);

  @ViewChild(ShipmentListComponent) list!: ShipmentListComponent;

  readonly importing = signal(false);
  readonly importJob = signal<ImportJob | null>(null);
  selectedFile: File | null = null;

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files && input.files.length > 0 ? input.files[0] : null;
  }

  upload(): void {
    if (!this.selectedFile) {
      return;
    }
    this.importing.set(true);
    this.importJob.set(null);
    this.shipmentService.importCsv(this.selectedFile).subscribe({
      next: (jobId) => this.poll(jobId),
      error: () => this.importing.set(false)
    });
  }

  private poll(jobId: string): void {
    this.shipmentService.getImportStatus(jobId).subscribe({
      next: (job) => {
        this.importJob.set(job);
        if (job.status === 'RUNNING') {
          setTimeout(() => this.poll(jobId), 1000);
        } else {
          this.importing.set(false);
          this.selectedFile = null;
          this.list.reload();
        }
      },
      error: () => this.importing.set(false)
    });
  }
}
