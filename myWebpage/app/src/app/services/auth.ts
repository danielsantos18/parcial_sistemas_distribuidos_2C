import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { jwtDecode } from 'jwt-decode';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(private http: HttpClient, private router: Router) {}

  // 🔹 Registro
  register(body: any): Observable<any> {
    return this.http.post('/api/register', body);
  }

  // 🔹 Login (con redirección automática)
  login(email: string, password: string): Observable<any> {
    const body = { email, password };
    return this.http.post('/api/login', body).pipe(
      tap((res: any) => {
        console.log('🟢 Respuesta del login:', res);
        if (res && res.token) {
          localStorage.setItem('token', res.token);
          console.log('💾 Token guardado en localStorage');
        } else {
          console.warn('⚠️ No se recibió token en la respuesta');
        }
      })
    );
  }

  // 🔹 Obtener token guardado
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  // 🔹 Saber si está logueado
  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }

  // 🔹 Cerrar sesión
  logout(): void {
    localStorage.removeItem('token');
    this.router.navigate(['/login']).then(() => {
      console.log('🚪 Sesión cerrada y redirigido a login');
    });
  }

  // 🔹 Decodificar el token
  getUserIdFromToken(): string | null {
    const token = this.getToken();
    if (!token) {
      console.warn('⚠️ No hay token guardado en localStorage');
      return null;
    }

    try {
      const decoded: any = jwtDecode(token);
      console.log('🔍 Token decodificado:', decoded);
      return decoded.sub || decoded.userId || null;
    } catch (error) {
      console.error('❌ Error al decodificar token:', error);
      return null;
    }
  }
  
}
