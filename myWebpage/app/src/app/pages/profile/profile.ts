import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ProfileService } from '../../services/profile.service';
import { AuthService } from '../../services/auth';

import { NavbarComponent } from '../../shared/components/navbar/navbar';

@Component({
  selector: 'app-profile',
  standalone: true, // ✅ sin app.module.ts
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './profile.html',
  styleUrls: ['./profile.scss']
})
export class ProfileComponent implements OnInit {
  user: any = null;
  tempUser: any = null;
  editMode = false;
  newPhoto: string | null = null;
  loading = false;

  constructor(
    private profileService: ProfileService,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadProfile();
  }

  // ✅ Cargar datos del perfil
  loadProfile() {
    this.loading = true;
    this.profileService.getProfile().subscribe({
      next: (res) => {
        this.loading = false;
        this.user = res;
        this.tempUser = { ...res };
        console.log('✅ Perfil cargado:', res);
      },
      error: (err) => {
        this.loading = false;
        console.error('❌ Error al cargar perfil:', err);
        alert('No se pudo cargar el perfil. Verifica tu sesión.');
      }
    });
  }

  // ✅ Alternar modo edición
  toggleEdit() {
    this.editMode = !this.editMode;
    this.tempUser = { ...this.user };
  }

  // ✅ Guardar cambios
  saveChanges() {
    this.loading = true;
    this.profileService.updateProfile(this.tempUser).subscribe({
      next: (res) => {
        this.loading = false;
        this.user = { ...this.tempUser };
        this.editMode = false;
        alert('✅ Perfil actualizado correctamente.');
      },
      error: (err) => {
        this.loading = false;
        console.error('❌ Error al actualizar perfil:', err);
        alert('No se pudo actualizar el perfil.');
      }
    });
  }

  // ✅ Cancelar edición
  cancelEdit() {
    this.editMode = false;
    this.tempUser = { ...this.user };
  }

  // ✅ Manejar foto seleccionada
  onPhotoSelected(event: any) {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      this.newPhoto = reader.result as string;
    };
    reader.readAsDataURL(file);
  }

  // ✅ Subir imagen
  uploadAvatar() {
    if (!this.newPhoto) {
      alert('Selecciona una imagen primero.');
      return;
    }

    const base64 = this.newPhoto.split(',')[1];
    const fileType = 'image/jpeg';

    this.loading = true;
    this.profileService.uploadAvatar(base64, fileType).subscribe({
      next: (res) => {
        this.loading = false;
        alert('✅ Foto actualizada correctamente.');
        this.user.foto = this.newPhoto;
      },
      error: (err) => {
        this.loading = false;
        console.error('❌ Error al subir imagen:', err);
        alert('No se pudo subir la imagen.');
      }
    });
  }

  // ✅ Cerrar sesión
  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
