// src/app/services/catalog.service.ts
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { environment } from '../environments/environment';

export interface ServiceItem {
  id: number;
  categoria: string;
  proveedor: string;
  servicio: string;
  plan: string;
  precio_mensual: number;
  detalles?: string;
  estado?: string;
}

@Injectable({ providedIn: 'root' })
export class CatalogService {
  // para desarrollo usamos mock; al conectar backend reemplaza por HttpClient.get(`${environment.apiBase}/catalog`)
  private mock: ServiceItem[] = [
    { id: 1, categoria: 'internet', proveedor: 'Claro', servicio: 'Fibra 200', plan: 'MENSUAL', precio_mensual: 120000, detalles: '200 Mbps simétricos', estado: 'activo' },
    { id: 2, categoria: 'energia', proveedor: 'EnergíaCo', servicio: 'Plan Hogar', plan: 'MENSUAL', precio_mensual: 85000, detalles: 'Factura mensual', estado: 'activo' },
    { id: 3, categoria: 'tv', proveedor: 'CableMax', servicio: 'Paquete Premium', plan: 'MENSUAL', precio_mensual: 65000, detalles: 'Incluye streaming', estado: 'activo' },
    { id: 3, categoria: 'sex on the bitch', proveedor: 'el daniel', servicio: 'chicas lindas ', plan: 'MENSUAL', precio_mensual: 50000, detalles: 'Incluye oral y vaginal', estado: 'activo' },
  ];

  constructor() {}

  getCatalog(): Observable<ServiceItem[]> {
    return of(this.mock);
  }

  // placeholder para cuando pases a backend:
  // getCatalog(): Observable<ServiceItem[]> {
  //   return this.http.get<ServiceItem[]>(`${environment.apiBase}/catalog`);
  // }
}
