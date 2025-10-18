import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private http: HttpClient, private router: Router) {}

  // ✅ LOGIN con backend AWS
    // ✅ LOGIN
  login(email: string, password: string): Observable<any> {
    const body = { email, password };
    return this.http.post(environment.apiLogin, body).pipe(
      tap((res: any) => {
        if (res && res.userId) {
          localStorage.setItem('user', JSON.stringify(res));
        }
      })
    );
  }

  // ✅ REGISTRO
  register(body: any) {
    console.log('📤 Enviando registro:', body);
    return this.http.post(environment.apiREgister, body);
  }




  // ✅ CERRAR SESIÓN
  logout(): void {
    localStorage.removeItem('user');
    this.router.navigate(['/login']);
  }

  // ✅ COMPROBAR SI HAY SESIÓN
  isAuthenticated(): boolean {
    return !!localStorage.getItem('user');
  }
}
