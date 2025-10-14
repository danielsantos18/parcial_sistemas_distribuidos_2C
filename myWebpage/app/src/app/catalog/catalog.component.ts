import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CatalogService } from '../services/catalog.service';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './catalog.component.html',
  styleUrls: ['./catalog.component.scss']
})
export class CatalogComponent implements OnInit {
  services: any[] = [];
  loading = true;

  constructor(private catalogService: CatalogService) {}

  ngOnInit() {
    this.catalogService.getCatalog().subscribe({
      next: (data) => {
        this.services = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al obtener catálogo', err);
        this.loading = false;
      }
    });
  }
}
