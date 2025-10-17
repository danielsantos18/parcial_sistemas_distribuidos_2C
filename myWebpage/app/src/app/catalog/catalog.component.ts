import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { CatalogService, ServiceItem } from '../services/catalog.service';
import { NavbarComponent } from '../shared/components/navbar/navbar';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatInputModule,
    MatButtonModule,
    NavbarComponent
  ],
  templateUrl: './catalog.component.html',
  styleUrls: ['./catalog.component.scss']
})
export class CatalogComponent implements OnInit {
  dataSource: ServiceItem[] = [];
  filteredData: ServiceItem[] = [];
  isLoading = true;
  errorMessage: string | null = null;
  searchTerm = '';

  constructor(private catalogService: CatalogService) {}

  ngOnInit(): void {
    this.loadCatalog();
  }

  loadCatalog(): void {
    this.isLoading = true;
    this.catalogService.getCatalog().subscribe({
      next: (data) => {
        this.dataSource = data;
        this.filteredData = data;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Error al cargar el catálogo.';
        this.isLoading = false;
      }
    });
  }

  filterCatalog(): void {
    const term = this.searchTerm.toLowerCase();
    this.filteredData = this.dataSource.filter(item =>
      (item.name?.toLowerCase() || item.servicio?.toLowerCase() || '').includes(term) ||
      (item.description?.toLowerCase() || item.detalles?.toLowerCase() || '').includes(term)
    );
  }

  pay(item: ServiceItem): void {
    const label = item.name || item.servicio || 'Servicio';
    const price = item.price ?? item.precio_mensual ?? 0;
    alert(`Simulando pago de ${label} - ${price}`);
  }

  viewDetails(item: ServiceItem): void {
    alert(`Detalles:\n${item.description || item.detalles || 'Sin descripción'}`);
  }
}
