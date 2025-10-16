import { Component } from '@angular/core';
import { AuthService } from '../../services/auth';
import { Router } from '@angular/router';


import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';


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


  constructor(private authService: AuthService, private router: Router) {}


  onLogin() {
    this.loading = true;
    this.authService.login(this.email, this.password).subscribe({
      next: (res: any) => {
      localStorage.setItem('user', JSON.stringify(res));
      this.router.navigate(['/profile']);
      },
      error: () => {
        alert('Invalid email or password');
        this.loading = false;
      }
    });
  }
}