// src/app/catalog/catalog.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatCheckboxModule } from '@angular/material/checkbox';

import { CatalogService, ServiceItem } from '../services/catalog.service';
import { NavbarComponent } from '../shared/components/navbar/navbar';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatInputModule,
    MatButtonModule,
    MatFormFieldModule,
    MatCheckboxModule,
    NavbarComponent
  ],
  templateUrl: './catalog.component.html',
  styleUrls: ['./catalog.component.scss']
})
export class CatalogComponent implements OnInit {
  displayedColumns: string[] = ['select', 'id', 'name', 'description', 'price', 'actions'];
  dataSource: ServiceItem[] = [];
  filteredData: ServiceItem[] = [];
  selectedServices: ServiceItem[] = [];
  searchTerm = '';
  isLoading = true;
  errorMessage: string | null = null;

  serviceForm: FormGroup;

  constructor(private catalogService: CatalogService, private fb: FormBuilder) {
    this.serviceForm = this.fb.group({
      id: ['', [Validators.required, Validators.pattern(/^\d+$/)]],
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required, Validators.minLength(5)]],
      price: ['', [Validators.required, Validators.min(0.01)]]
    });
  }

  ngOnInit(): void {
    this.loadCatalog();
  }

  loadCatalog(): void {
    this.isLoading = true;
    this.catalogService.getCatalog().subscribe({
      next: (data: ServiceItem[]) => {
        this.dataSource = data.map(d => this.normalize(d));
        this.filteredData = [...this.dataSource];
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error fetching catalog', err);
        this.errorMessage = 'Error al cargar el catálogo. Intenta nuevamente.';
        this.isLoading = false;
      }
    });
  }

  // Unificamos formatos para evitar undefined en templates
  private normalize(i: ServiceItem): ServiceItem {
    return {
      id: Number(i.id),
      name: i.name ?? i.servicio ?? `Servicio ${i.id}`,
      description: i.description ?? i.detalles ?? '',
      price: i.price ?? i.precio_mensual ?? 0,
      servicio: i.servicio,
      proveedor: i.proveedor,
      plan: i.plan,
      precio_mensual: i.precio_mensual,
      detalles: i.detalles,
      estado: i.estado
    };
  }

  filterCatalog(): void {
    const t = this.searchTerm.trim().toLowerCase();
    if (!t) { this.filteredData = [...this.dataSource]; return; }
    this.filteredData = this.dataSource.filter(item =>
      (item.name?.toLowerCase() || '').includes(t) ||
      (item.description?.toLowerCase() || '').includes(t) ||
      String(item.price ?? '').includes(t)
    );
  }

  /* ------------------ Selección ------------------ */
  onCheckboxChange(service: ServiceItem, isChecked: boolean): void {
    if (isChecked) {
      if (!this.isSelected(service)) this.selectedServices.push(service);
    } else {
      this.selectedServices = this.selectedServices.filter(s => s.id !== service.id);
    }
  }

  isSelected(service: ServiceItem): boolean {
    return this.selectedServices.some(s => s.id === service.id);
  }

  /* ------------------ Agregar servicio ------------------ */
  addService(): void {
    if (this.serviceForm.invalid) {
      this.serviceForm.markAllAsTouched();
      alert('Por favor completa correctamente el formulario.');
      return;
    }

    const form = this.serviceForm.value;
    const maxId = this.dataSource.length ? Math.max(...this.dataSource.map(s => s.id)) : 0;
    const newService: ServiceItem = this.normalize({
      id: Number(form.id) || (maxId + 1),
      name: form.name,
      description: form.description,
      price: Number(form.price),
      servicio: form.name,
      precio_mensual: Number(form.price)
    });

    // Añadir a las listas locales (mock). Si tuvieras backend, aquí llamas al servicio.
    this.dataSource = [...this.dataSource, newService];
    this.filteredData = [...this.filteredData, newService];
    this.serviceForm.reset();
    alert(`Servicio ${newService.name} añadido con éxito`);
  }

  /* ------------------ Eliminar ------------------ */
  deleteService(service: ServiceItem): void {
    if (!confirm(`¿Eliminar ${service.name} (ID: ${service.id})?`)) return;
    this.dataSource = this.dataSource.filter(s => s.id !== service.id);
    this.filteredData = this.filteredData.filter(s => s.id !== service.id);
    this.selectedServices = this.selectedServices.filter(s => s.id !== service.id);
    alert(`Servicio ${service.name} eliminado`);
  }

  /* ------------------ Export CSV ------------------ */
  exportToCSV(): void {
    if (this.selectedServices.length === 0) {
      alert('Selecciona al menos un servicio para exportar');
      return;
    }

    const headers = ['ID', 'Nombre', 'Descripción', 'Precio'];
    const csvRows = [
      headers.join(','),
      ...this.selectedServices.map(s =>
        `"${s.id}","${(s.name || '').replace(/"/g, '""')}","${(s.description || '').replace(/"/g, '""')}","${s.price}"`
      )
    ];
    const csvContent = csvRows.join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', 'catalogo_seleccionado.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  iniciarPago(service: ServiceItem): void {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    const cardId = user.cardId; // o selecciona la tarjeta del perfil

    this.catalogService.payService(cardId, service).subscribe({
      next: res => alert(`Pago iniciado con traceId: ${res.traceId}`),
      error: err => alert('Error al iniciar el pago.'),
    });
  }


}
