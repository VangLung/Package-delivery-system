import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  username = '';
  password = '';
  errorMessage = '';
  loading = false;

  constructor(private auth: AuthService, private router: Router) {}

  onLogin(): void {
    if (!this.username || !this.password) {
      this.errorMessage = 'Please enter your username and password.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.auth.login(this.username, this.password).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['']);
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Invalid username or password.';
      },
    });
  }
}
