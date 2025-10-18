import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth';
import { Router,RouterModule } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class LoginComponent {
  email = '';
  password = '';
  loading = false;

  constructor(private auth: AuthService, private router: Router) {}

  // ✅ Ahora coincide con tu HTML (ngSubmit)="submit()"
  submit() {
    if (!this.email || !this.password) {
      alert('⚠️ Ingresa tu correo y contraseña.');
      return;
    }

    this.loading = true;

    this.auth.login(this.email, this.password).subscribe({
      next: (res) => {
        this.loading = false;
        if (res && res.token) {
          alert('✅ Inicio de sesión exitoso.');
          this.router.navigate(['/home']);
        } else {
          alert('⚠️ No se recibió token. Revisa la respuesta del servidor.');
          console.log('Respuesta del servidor:', res);
        }
      },
      error: (err) => {
        this.loading = false;
        console.error('❌ Error en login:', err);
        if (err.status === 400 || err.status === 401) {
          alert('⚠️ Credenciales inválidas. Verifica tu email y contraseña.');
        } else {
          alert('❌ Error inesperado. Revisa la consola.');
        }
      }
    });
  }


}
