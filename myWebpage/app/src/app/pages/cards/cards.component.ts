import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../shared/components/navbar/navbar';

@Component({
  selector: 'app-cards',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
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
      saldo: 1200000,
      color: 'linear-gradient(135deg, #06b6d4, #0f172a)',
      activa: true
    },
    {
      id: 2,
      tipo: 'InfernoCard Gold',
      numero: '**** **** **** 9987',
      titular: 'Yefrey Salazar',
      expira: '12/27',
      saldo: 560000,
      color: 'linear-gradient(135deg, #ef4444, #701a1a)',
      activa: false
    }
  ];

  selected = this.cards[0];
  recarga = 0;

  toggle(card: any) {
    card.activa = !card.activa;
  }

  select(card: any) {
    this.selected = card;
  }

  recargar() {
    if (this.recarga <= 0) {
      window.alert('Por favor, ingresa un monto válido.');
      return;
    }
    this.selected.saldo += this.recarga;
    window.alert(`Se recargaron ${this.recarga.toLocaleString()} COP a ${this.selected.tipo}`);
    this.recarga = 0;
  }

  nuevaTarjeta() {
    window.alert('Funcionalidad próximamente');
  }
}
