import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../shared/components/navbar/navbar';

@Component({
  selector: 'app-payments',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './payments.component.html',
  styleUrls: ['./payments.component.scss']
})
export class PaymentsComponent {
  pagos = [
    { servicio: 'Internet Hogar', monto: 120000, fecha: '2025-10-15', traceId: 'TRX-12345', estado: 'Aprobado' },
    { servicio: 'EnergíaCo', monto: 85000, fecha: '2025-10-10', traceId: 'TRX-67890', estado: 'Pendiente' },
  ];
}
