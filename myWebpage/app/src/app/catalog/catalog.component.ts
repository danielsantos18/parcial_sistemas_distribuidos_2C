import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CatalogService, ServiceItem } from '../services/catalog.service'

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatProgressSpinnerModule],
  templateUrl: './catalog.component.html',
  styleUrls: ['./catalog.component.css']
})
export class CatalogComponent implements OnInit {
  displayedColumns: string[] = ['id', 'name', 'description', 'price']; 
  dataSource: ServiceItem[] = [];
  isLoading = true;
  errorMessage: string | null = null;

  constructor(private catalogService: CatalogService) {}

  ngOnInit(): void {
    this.loadCatalog();
  }

loadCatalog(): void {
  this.isLoading = true;
  this.catalogService.getCatalog().subscribe({
    next: (data: ServiceItem[]) => {  
      this.dataSource = data;
      this.isLoading = false;
    },
    error: (error: any) => { 
      this.errorMessage = 'Error al cargar el catálogo. Intenta nuevamente.';
      this.isLoading = false;
      console.error('Error fetching catalog:', error);
    }
  });
}
}