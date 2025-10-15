import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs'; 

export interface ServiceItem {
  id: string;
  name: string;
  description: string;
  price: number;
}

@Injectable({
  providedIn: 'root'
})
export class CatalogService {
  // Endpoint placeholder
  private apiUrl = 'https://your-api-gateway-url/catalog';

  constructor(private http: HttpClient) {}

  getCatalog(): Observable<ServiceItem[]> {
    return of([
      { id: '1', name: 'Servicio A', description: 'Descripción de prueba A', price: 10.99 },
      { id: '2', name: 'Servicio B', description: 'Descripción de prueba B', price: 20.50 },
      { id: '3', name: 'Servicio C', description: 'Descripción de prueba C', price: 15.00 }
    ]);
  }
}