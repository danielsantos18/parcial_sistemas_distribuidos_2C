// src/app/services/catalog.service.ts
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

/**
 * Interfaz que incluye los campos usados por Home (servicio/precio_mensual)
 * y por el Catalog original (name/description/price). Así mantenemos compatibilidad.
 */
export interface ServiceItem {
  id: number;
  // formato "catalog (nuevo/amigo)"
  name?: string;
  description?: string;
  price?: number;
  // formato "home (original)"
  servicio?: string;
  proveedor?: string;
  plan?: string;
  precio_mensual?: number;
  detalles?: string;
  estado?: string;
}

@Injectable({ providedIn: 'root' })
export class CatalogService {
  // mock seguro y limpio (he eliminado entradas inadecuadas)
  private mock: ServiceItem[] = [
  {
    id: 1,
    servicio: 'Fibra 200',
    proveedor: 'Claro',
    plan: 'MENSUAL',
    precio_mensual: 120000,
    name: 'Fibra 200',
    description: '200 Mbps simétricos de Internet de alta velocidad.',
    price: 120000,
    estado: 'activo'
  },
  {
    id: 2,
    servicio: 'Plan Hogar Energía',
    proveedor: 'EnergíaCo',
    plan: 'MENSUAL',
    precio_mensual: 85000,
    name: 'Plan Hogar Energía',
    description: 'Servicio de energía eléctrica mensual para el hogar.',
    price: 85000,
    estado: 'activo'
  },
  {
    id: 3,
    servicio: 'Paquete Premium TV',
    proveedor: 'CableMax',
    plan: 'MENSUAL',
    precio_mensual: 65000,
    name: 'Paquete Premium TV',
    description: 'Incluye canales HD, deportes y streaming.',
    price: 65000,
    estado: 'activo'
  },
  {
    id: 4,
    servicio: 'Agua Potable Residencial',
    proveedor: 'Acueductos S.A.',
    plan: 'MENSUAL',
    precio_mensual: 40000,
    name: 'Agua Potable',
    description: 'Servicio básico de agua potable para el hogar.',
    price: 40000,
    estado: 'activo'
  },
  {
    id: 5,
    servicio: 'Gas Natural Familiar',
    proveedor: 'GasSur',
    plan: 'MENSUAL',
    precio_mensual: 55000,
    name: 'Gas Natural Hogar',
    description: 'Suministro de gas natural para uso residencial.',
    price: 55000,
    estado: 'activo'
  },
  {
    id: 6,
    servicio: 'Plan Móvil 20GB',
    proveedor: 'MoviTel',
    plan: 'MENSUAL',
    precio_mensual: 75000,
    name: 'Plan Móvil 20GB',
    description: 'Datos móviles 20GB, minutos y SMS ilimitados.',
    price: 75000,
    estado: 'activo'
  },
  {
    id: 7,
    servicio: 'Streaming Plus',
    proveedor: 'CineFlix',
    plan: 'MENSUAL',
    precio_mensual: 45000,
    name: 'Streaming Plus',
    description: 'Plataforma de streaming con miles de películas y series.',
    price: 45000,
    estado: 'activo'
  },
  {
    id: 8,
    servicio: 'Mantenimiento Hogar',
    proveedor: 'HomeCare',
    plan: 'ANUAL',
    precio_mensual: 300000,
    name: 'Mantenimiento Hogar',
    description: 'Servicio anual de mantenimiento eléctrico y de plomería.',
    price: 300000,
    estado: 'activo'
  },
  {
    id: 9,
    servicio: 'Alarma y Seguridad',
    proveedor: 'SafeHouse',
    plan: 'MENSUAL',
    precio_mensual: 95000,
    name: 'Seguridad Residencial',
    description: 'Sistema de alarma, cámaras y monitoreo 24/7.',
    price: 95000,
    estado: 'activo'
  },
  {
    id: 10,
    servicio: 'Plan Salud Básico',
    proveedor: 'MediPlus',
    plan: 'MENSUAL',
    precio_mensual: 120000,
    name: 'Seguro Médico Básico',
    description: 'Cobertura en consultas generales y emergencias.',
    price: 120000,
    estado: 'activo'
  }
];


  constructor() {}

  getCatalog(): Observable<ServiceItem[]> {
    return of(this.mock);
  }
}
