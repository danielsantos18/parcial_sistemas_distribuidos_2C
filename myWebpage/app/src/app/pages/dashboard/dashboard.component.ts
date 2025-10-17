import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../shared/components/navbar/navbar';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  user = { name: 'Usuario Demo', saldo: 1450000 };
  recent = [
    { servicio: 'Internet Hogar', monto: 120000, fecha: '2025-10-15' },
    { servicio: 'EnergíaCo', monto: 85000, fecha: '2025-10-10' },
    { servicio: 'Netflix', monto: 45000, fecha: '2025-10-07' },
  ];

  ngOnInit() {}
}
