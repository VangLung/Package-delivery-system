import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService, RegisterRequest, Role } from '../services/auth.service';

@Component({
  selector: 'app-register',
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  model: RegisterRequest = {
    username: '',
    password: '',
    firstName: '',
    lastName: '',
    email: '',
    role: 'user',
  };

  roles: Role[] = ['user', 'courier', 'admin'];
  errorMessage = '';
  loading = false;

  constructor(private auth: AuthService, private router: Router) {}

  onRegister(): void {
    const { username, password, firstName, lastName, email } = this.model;
    if (!username || !password || !firstName || !lastName || !email) {
      this.errorMessage = 'All fields are required.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.auth.register(this.model).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage =
          typeof err?.error === 'string' ? err.error : 'Registration failed. Please try again.';
      },
    });
  }
}
