import {
  Component, Input, ViewChild, ElementRef,
  AfterViewInit, OnDestroy, inject, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ShipmentService, Shipment } from '../services/shipment.service';

@Component({
  selector: 'app-shipment-list',
  imports: [CommonModule],
  templateUrl: './shipment-list.html'
})
export class ShipmentListComponent implements AfterViewInit, OnDestroy {
  private shipmentService = inject(ShipmentService);

  @Input() title = 'Shipments';
  @Input() canChangeStatus = false;
  @Input() excludeDelivered = false;

  @ViewChild('sentinel') sentinel!: ElementRef<HTMLElement>;

  readonly items = signal<Shipment[]>([]);
  readonly loading = signal(false);

  private cursor: number | undefined;
  private hasMore = true;
  private observer?: IntersectionObserver;
  private readonly limit = 50;

  ngAfterViewInit(): void {
    this.observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) {
        this.loadMore();
      }
    });
    this.observer.observe(this.sentinel.nativeElement);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  reload(): void {
    this.items.set([]);
    this.cursor = undefined;
    this.hasMore = true;
    this.loadMore();
  }

  loadMore(): void {
    if (this.loading() || !this.hasMore) {
      return;
    }
    this.loading.set(true);
    this.shipmentService.searchShipments(this.cursor, this.limit).subscribe({
      next: (batch) => {
        this.items.update((cur) => [...cur, ...batch]);
        if (batch.length < this.limit) {
          this.hasMore = false;
        }
        if (batch.length > 0) {
          this.cursor = batch[batch.length - 1].id;
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  changeStatus(item: Shipment, status: string): void {
    if (!item.id) {
      return;
    }
    this.shipmentService.updateStatus(item.id, status).subscribe({
      next: (ok) => {
        if (!ok) {
          return;
        }
        if (this.excludeDelivered && status === 'DELIVERED') {
          this.items.update((cur) => cur.filter((s) => s.id !== item.id));
        } else {
          this.items.update((cur) =>
            cur.map((s) => (s.id === item.id ? { ...s, currentStatus: status } : s)));
        }
      }
    });
  }
}
