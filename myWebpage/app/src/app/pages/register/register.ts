import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.html',
  styleUrls: ['./register.scss']
})
export class RegisterComponent {
  name = '';
  lastName = '';
  email = '';
  password = '';
  document = '';
  loading = false;

  constructor(private auth: AuthService, private router: Router) {}

  submit() {
    if (!this.name || !this.lastName || !this.email || !this.password || !this.document) {
      alert('⚠️ Por favor completa todos los campos.');
      return;
    }

    if (this.password.length < 8) {
      alert('⚠️ La contraseña debe tener al menos 8 caracteres.');
      return;
    }

    this.loading = true;

    this.auth.register({
      name: this.name,
      lastName: this.lastName,
      email: this.email,
      password: this.password,
      document: this.document
    }).subscribe({
      next: (res) => {
        this.loading = false;
        console.log('✅ Registro exitoso:', res);
        alert('✅ Registro exitoso. Ahora puedes iniciar sesión.');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.loading = false;
        console.error('❌ Error del servidor:', err);
        if (err.error?.message) {
          alert('❌ ' + err.error.message);
        } else if (err.status === 400) {
          alert('⚠️ Verifica que la contraseña tenga mínimo 8 caracteres y que el email no exista.');
        } else {
          alert('❌ Error al registrarse. Revisa la consola para más detalles.');
        }
      }
    });
  }

}
