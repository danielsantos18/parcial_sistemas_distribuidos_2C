import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../shared/components/navbar/navbar';

interface Movimiento {
  fecha: string;
  descripcion: string;
  monto: number;
  tipo: 'entrada' | 'salida';
}

interface Cuenta {
  tipo: string;
  numero: string;
  saldo: number;
}

interface Usuario {
  nombre: string;
  correo: string;
  cedula: string;
  telefono: string;
  direccion: string;
  foto: string;
  cuentas: Cuenta[];
  movimientos: Movimiento[];
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './profile.html',
  styleUrls: ['./profile.scss']
})
export class ProfileComponent implements OnInit {
  user: Usuario | null = null;
  editMode = false;
  tempUser: Usuario | null = null;
  newPhoto: string | null = null;

  ngOnInit() {
    // Simula datos del backend
    this.user = {
      nombre: 'Yefrey Salazar',
      correo: 'yefrey@example.com',
      cedula: '1098463721',
      telefono: '+57 300 589 2221',
      direccion: 'Cra 14 #12 - 45, Cartagena',
      foto: 'https://cdn-icons-png.flaticon.com/512/3135/3135715.png',
      cuentas: [
        { tipo: 'Cuenta de Ahorros', numero: '4550-1234-9876', saldo: 2300000 },
        { tipo: 'Cuenta Corriente', numero: '4111-9876-5543', saldo: 870000 }
      ],
      movimientos: [
        { fecha: '2025-10-12', descripcion: 'Pago tienda Éxito', monto: -85000, tipo: 'salida' },
        { fecha: '2025-10-11', descripcion: 'Transferencia recibida', monto: 150000, tipo: 'entrada' },
        { fecha: '2025-10-09', descripcion: 'Pago Netflix', monto: -23000, tipo: 'salida' },
        { fecha: '2025-10-08', descripcion: 'Recarga tarjeta', monto: 50000, tipo: 'entrada' }
      ]
    };
  }

  toggleEdit() {
    this.editMode = !this.editMode;
    this.tempUser = { ...(this.user as Usuario) };
    this.newPhoto = null;
  }

  onPhotoSelected(event: any) {
    const file = event.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => (this.newPhoto = reader.result as string);
    reader.readAsDataURL(file);
  }

  saveChanges() {
    if (this.newPhoto) {
      this.tempUser!.foto = this.newPhoto;
    }
    this.user = { ...(this.tempUser as Usuario) };
    this.editMode = false;
    alert('✅ Perfil actualizado con éxito');
  }

  cancelEdit() {
    this.editMode = false;
  }

  logout() {
    localStorage.clear();
    window.location.href = '/login';
  }
}
