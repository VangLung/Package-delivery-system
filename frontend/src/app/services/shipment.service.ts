import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Shipment {
  id?: number;
  trackingNumber: string;
  description: string;
  currentStatus: string;
  customerUsername: string;
  createdAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ShipmentService {
  private apiUrl = 'http://localhost:8080/shipments';

  constructor(private http: HttpClient) {}

  createShipment(shipment: Shipment): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/create`, shipment);
  }

  updateStatus(id: number, status: string): Observable<boolean> {
    const params = new HttpParams().set('id', id.toString()).set('status', status);
    return this.http.post<boolean>(`${this.apiUrl}/update-status`, null, { params });
  }

  searchShipments(customer?: string, status?: string, date?: string): Observable<Shipment[]> {
    let params = new HttpParams();
    if (customer) params = params.set('customer', customer);
    if (status) params = params.set('status', status);
    if (date) params = params.set('date', date);

    return this.http.get<Shipment[]>(`${this.apiUrl}/search`, { params });
  }
}