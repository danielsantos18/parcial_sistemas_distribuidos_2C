import { Component } from '@angular/core';
import { AuthService } from '../../services/auth';
import { Router } from '@angular/router';


import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';


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
  loading = false;


  constructor(private authService: AuthService, private router: Router) {}


  onRegister() {
    this.loading = true;
    const data = { name: this.name, lastName: this.lastName, email: this.email, password: this.password };


    this.authService.register(data).subscribe({
      next: () => {
        alert('Account created successfully!');
        this.router.navigate(['/login']);
      },
      error: () => {
        alert('Error during registration');
        this.loading = false;
      }
    });
  }
}