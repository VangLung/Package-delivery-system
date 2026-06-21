import { Routes } from '@angular/router';
import { ControlPanelComponent } from './control-panel/control-panel';
import { Login } from './login/login';
import { Register } from './register/register';
import { authGuard } from './guards/auth-guard';

export const routes: Routes = [
    { path: 'login', component: Login },
    { path: 'register', component: Register },
    { path: '', component: ControlPanelComponent, canActivate: [authGuard] },
    { path: '**', redirectTo: '' },
];
