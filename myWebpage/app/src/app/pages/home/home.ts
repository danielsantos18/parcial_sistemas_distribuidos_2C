import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, NavbarComponent, RouterModule],
  templateUrl: './home.html',
  styleUrls: ['./home.scss']
})
export class HomeComponent implements OnInit {
  recentTransfers = [
    { id: 1, destinatario: 'Internet Hogar', monto: 120000, fecha: '2025-10-15' },
    { id: 2, destinatario: 'María Gómez', monto: 75000, fecha: '13/10/2025' },
    { id: 3, destinatario: 'Electricaribe', monto: 95000, fecha: '10/10/2025' }
  ];

  constructor(private router: Router) {}

  ngOnInit(): void {}

  goTo(route: string): void {
    this.router.navigate([route]);
  }
}
