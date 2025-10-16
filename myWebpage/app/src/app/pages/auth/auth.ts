import { Component } from '@angular/core';
import { AuthService } from '../../services/auth';
import { Router } from '@angular/router';


import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';


@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './auth.html',
  styleUrls: ['./auth.scss']
})
export class AuthComponent {
  isSignUp = false;
  loading = false;


  // login form
  email = '';
  password = '';


  // register form
  name = '';
  lastName = '';
  regEmail = '';
  regPassword = '';


  constructor(private authService: AuthService, private router: Router) {}


  toggleForm() { this.isSignUp = !this.isSignUp; }


  onLogin() {
    this.loading = true;
    this.authService.login(this.email, this.password).subscribe({
      next: (res: any) => {
        localStorage.setItem('user', JSON.stringify(res));
        this.router.navigate(['/home']);
      },
      error: () => {
        alert('Invalid credentials');
        this.loading = false;
      }
    });
  }


  onRegister() {
    this.loading = true;
    const data = { name: this.name, lastName: this.lastName, email: this.regEmail, password: this.regPassword };
    this.authService.register(data).subscribe({
      next: () => {
        alert('Account created successfully!');
        this.toggleForm();
        this.loading = false;
      },
      error: () => {
        alert('Error during registration');
        this.loading = false;
      }
    });
  }
}