import {
  Component, Input, ViewChild, ElementRef,
  AfterViewInit, OnDestroy, inject, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ShipmentService, Shipment, StatusLog, ShipmentFilters } from '../services/shipment.service';

@Component({
  selector: 'app-shipment-list',
  imports: [CommonModule, FormsModule],
  templateUrl: './shipment-list.html'
})
export class ShipmentListComponent implements AfterViewInit, OnDestroy {
  private shipmentService = inject(ShipmentService);

  @Input() title = 'Shipments';
  @Input() canChangeStatus = false;
  @Input() excludeDelivered = false;
  @Input() canFilterByCustomer = false;

  @ViewChild('sentinel') sentinel!: ElementRef<HTMLElement>;

  readonly items = signal<Shipment[]>([]);
  readonly loading = signal(false);

  filterCustomer = '';
  filterStatus = '';
  filterDate = '';

  // History modal state
  readonly historyFor = signal<Shipment | null>(null);
  readonly history = signal<StatusLog[]>([]);

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

  applyFilters(): void {
    this.reload();
  }

  resetFilters(): void {
    this.filterCustomer = '';
    this.filterStatus = '';
    this.filterDate = '';
    this.reload();
  }

  loadMore(): void {
    if (this.loading() || !this.hasMore) {
      return;
    }
    this.loading.set(true);
    const filters: ShipmentFilters = {
      customer: this.filterCustomer || undefined,
      status: this.filterStatus || undefined,
      date: this.filterDate || undefined
    };
    this.shipmentService.searchShipments(this.cursor, this.limit, filters).subscribe({
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

  openHistory(item: Shipment): void {
    if (!item.id) {
      return;
    }
    this.historyFor.set(item);
    this.history.set([]);
    this.shipmentService.getHistory(item.id).subscribe({
      next: (logs) => this.history.set(logs)
    });
  }

  closeHistory(): void {
    this.historyFor.set(null);
    this.history.set([]);
  }
}
