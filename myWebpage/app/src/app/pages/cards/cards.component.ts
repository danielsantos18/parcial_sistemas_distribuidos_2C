import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../shared/components/navbar/navbar';

@Component({
  selector: 'app-cards',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './cards.component.html',
  styleUrls: ['./cards.component.scss']
})
export class CardsComponent {
  cards = [
    {
      id: 1,
      tipo: 'InfernoCard Platinum',
      numero: '**** **** **** 4321',
      titular: 'Yefrey Salazar',
      expira: '08/28',
      color: 'linear-gradient(135deg, #06b6d4, #0f172a)',
      activa: true
    },
    {
      id: 2,
      tipo: 'InfernoCard Gold',
      numero: '**** **** **** 9987',
      titular: 'Yefrey Salazar',
      expira: '12/27',
      color: 'linear-gradient(135deg, #ef4444, #701a1a)',
      activa: false
    }
  ];

  toggle(card: any) {
    card.activa = !card.activa;
  }

  addCard() {
    alert('Funcionalidad de creación de tarjeta próximamente');
  }
}
