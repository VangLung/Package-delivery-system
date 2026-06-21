import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserPanelComponent } from '../user-panel/user-panel';
import { AdminPanelComponent } from '../admin-panel/admin-panel';
import { CourierPanelComponent } from '../courier-panel/courier-panel';

@Component({
  selector: 'app-control-panel',
  templateUrl: './control-panel.html',
  styleUrls: ['./control-panel.css'],
  imports: [UserPanelComponent, AdminPanelComponent, CourierPanelComponent]
})
export class ControlPanelComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  readonly currentUser = this.authService.currentUser;
  readonly role = this.authService.role;

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}