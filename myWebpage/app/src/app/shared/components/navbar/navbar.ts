import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <nav class="nav">
      <div class="brand" (click)="goHome()">Inferno Bank</div>

      <div class="links">
        <a routerLink="/home" routerLinkActive="active">Home</a>
        <a routerLink="/profile" routerLinkActive="active">Profile</a>
        <button class="logout" (click)="logout()">Logout</button>
      </div>
    </nav>
  `,
  styles: [`
    .nav{
      display:flex;
      justify-content:space-between;
      align-items:center;
      padding:0.8rem 1.5rem;
      background: linear-gradient(90deg,#141e30,#243b55);
      color:white;
      box-shadow:0 6px 18px rgba(0,0,0,0.3);
    }
    .brand{ font-weight:800; cursor:pointer; }
    .links{ display:flex; gap:1rem; align-items:center; }
    a{ color:rgba(255,255,255,0.9); text-decoration:none; }
    a.active{ text-decoration:underline; }
    .logout{
      background:#ff4d4d; border:none; color:white; padding:0.4rem 0.8rem; border-radius:8px; cursor:pointer;
    }
  `]
})
export class NavbarComponent {
  constructor(private router: Router) {}

  logout(){
    localStorage.removeItem('user');
    this.router.navigate(['/auth']);
  }

  goHome(){
    this.router.navigate(['/home']);
  }
}
