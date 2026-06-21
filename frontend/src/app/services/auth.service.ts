import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export type Role = 'admin' | 'user' | 'courier';

export interface AuthUser {
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
}

export interface RegisterRequest {
  username: string;
  password: string;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
}

const STORAGE_KEY = 'auth_user';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/auth';

  private readonly _currentUser = signal<AuthUser | null>(this.loadFromStorage());

  readonly currentUser = this._currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this._currentUser() !== null);
  readonly role = computed<Role | null>(() => this._currentUser()?.role ?? null);

  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${this.apiUrl}/login`, { username, password }).pipe(
      tap((user) => this.setCurrentUser(user))
    );
  }

  register(payload: RegisterRequest): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${this.apiUrl}/register`, payload);
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this._currentUser.set(null);
  }

  private setCurrentUser(user: AuthUser): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
    this._currentUser.set(user);
  }

  private loadFromStorage(): AuthUser | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}
