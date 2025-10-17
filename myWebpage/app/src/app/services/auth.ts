// src/app/services/auth.service.ts
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

export interface User {
  id: string;
  name: string;
  email: string;
  token?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private mockUser: User = { id: 'u1', name: 'Usuario Demo', email: 'demo@ejemplo.com', token: 'mock-token-123' };

  constructor() {}

  login(email: string, password: string): Observable<User> {
    // mock: acepta cualquier credencial; cuando tengas backend, usa HttpClient.post(...)
    return of(this.mockUser);
  }

  register(name: string, email: string, password: string): Observable<User> {
    return of({ id: 'u2', name, email, token: 'mock-token-register' });
  }

  getProfile(): Observable<User> {
    return of(this.mockUser);
  }

  logout() {
    // limpia localStorage en la app real
    localStorage.removeItem('user');
  }
}
