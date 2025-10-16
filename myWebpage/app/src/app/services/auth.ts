import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs';

export interface UserProfile {
  id?: string;
  name?: string;
  email?: string;
  // add other known fields here
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private registerUrl = 'https://sdamtp8yo3.execute-api.us-east-2.amazonaws.com/users/register';
  private profileUrl = 'https://v7hjhcn0ej.execute-api.us-east-2.amazonaws.com/users/profile';
  // ⚠️ You don't have a specific login endpoint in the JSON, so we can simulate login by checking existing users or mocking it.

  constructor(private http: HttpClient) {}

  register(data: any): Observable<any> {
    return this.http.post(this.registerUrl, data);
  }

  // mock login example — replace later with your real backend login endpoint
  login(email: string, password: string): Observable<UserProfile> {
    // simulate authentication by returning the profile resource (typed)
    return this.http.get<UserProfile>(`${this.profileUrl}/e25851fb-b263-48e9-bb97-90b27c2a8bd8`);
  }

  getProfile(userId: string): Observable<any> {
    return this.http.get(`${this.profileUrl}/${userId}`);
  }

  updateProfile(userId: string, data: any): Observable<any> {
    const url = `https://dppejwy8a1.execute-api.us-east-2.amazonaws.com/users/profile/${userId}`;
    return this.http.put(url, data);
  }
}
