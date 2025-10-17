// src/app/pages/home/home.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CatalogService, ServiceItem } from '../../services/catalog.service';
import { NavbarComponent } from '../../shared/components/navbar/navbar';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './home.html',
  styleUrls: ['./home.scss']
})
export class HomeComponent implements OnInit {
  services: ServiceItem[] = [];
  selected: ServiceItem | null = null;
  loading = false;

  constructor(private catalog: CatalogService) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.catalog.getCatalog().subscribe(list => {
      this.services = list;
      this.loading = false;
    });
  }

  select(s: ServiceItem) {
    this.selected = s;
  }

  // mock de inicio de pago visual
  pay(service: ServiceItem) {
    alert(`Simulando pago de ${service.servicio} - ${service.precio_mensual}`);
  }
}
