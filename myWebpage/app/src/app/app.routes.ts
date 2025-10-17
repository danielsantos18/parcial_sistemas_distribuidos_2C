// src/app/app.routes.ts
import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', loadComponent: () => import('./pages/home/home').then(c => c.HomeComponent) },
  { path: 'login', loadComponent: () => import('./pages/login/login').then(c => c.LoginComponent) },
  { path: 'catalog', loadComponent: () => import('./catalog/catalog.component').then(c => c.CatalogComponent) },
  { path: 'profile', loadComponent: () => import('./pages/profile/profile').then(c => c.ProfileComponent) },
  { path: 'register', loadComponent: () => import('./pages/register/register').then(c => c.RegisterComponent) },
  { path: 'dashboard', loadComponent: () => import('../app/pages/dashboard/dashboard.component').then(c => c.DashboardComponent) },
  { path: 'cards', loadComponent: () => import('../app/pages/cards/cards.component').then(c => c.CardsComponent) },
  { path: 'payments', loadComponent: () => import('../app/pages/payments/payments.component').then(c => c.PaymentsComponent) },
  { path: 'support', loadComponent: () => import('../app/pages/support/support.component').then(c => c.SupportComponent) },
  { path: '**', redirectTo: 'home' }
];
