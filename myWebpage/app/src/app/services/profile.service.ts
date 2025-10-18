import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth'; // 👈 Asegúrate de importar tu servicio de auth

@Injectable({ providedIn: 'root' })
export class ProfileService {
  constructor(private http: HttpClient, private auth: AuthService) {}

  // ✅ Aquí está la parte que debes modificar
  getProfile(): Observable<any> {
    const token = this.auth.getToken(); // toma el token del localStorage
    const userId = this.auth.getUserIdFromToken(); // decodifica el id del usuario

    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`
    });

    // 🔥 Esta es la línea que debes tener:
    return this.http.get(`/api/users/profile/${userId}`, { headers });
  }

  updateProfile(data: any): Observable<any> {
    const token = this.auth.getToken();
    const userId = this.auth.getUserIdFromToken();

    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`
    });

    return this.http.put(`/api/users/profile/${userId}`, data, { headers });
  }

  uploadAvatar(imageBase64: string, fileType: string): Observable<any> {
    const token = this.auth.getToken();
    const userId = this.auth.getUserIdFromToken();

    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`
    });

    return this.http.post(
      `/api/users/${userId}/avatar`,
      { image: imageBase64, fileType },
      { headers }
    );
  }
}
