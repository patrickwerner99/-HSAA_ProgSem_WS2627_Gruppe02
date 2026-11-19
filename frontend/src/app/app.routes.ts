import { Routes } from '@angular/router';
import { NotFoundPage } from './pages/not-found/not-found';
import { ProductListPage } from './pages/product-list/product-list';
import { adminGuard, authGuard } from './shared/auth';

// Die Startseite wird sofort geladen, alle anderen Seiten erst bei Bedarf (kleinerer Start-Download).
// Jede Ansicht hat eine eigene Adresse, Filter stehen als ?… dahinter (C2).
export const routes: Routes = [
  { path: '', component: ProductListPage, title: 'Hyrox-Shop' },
  { path: 'sale', component: ProductListPage, data: { sale: true }, title: 'Sale | Hyrox-Shop' },
  {
    path: 'produkte/:id',
    loadComponent: () =>
      import('./pages/product-detail/product-detail').then((m) => m.ProductDetailPage),
    title: 'Produkt | Hyrox-Shop',
  },
  {
    path: 'warenkorb',
    loadComponent: () => import('./pages/cart/cart').then((m) => m.CartPage),
    title: 'Warenkorb | Hyrox-Shop',
  },
  {
    path: 'anmelden',
    loadComponent: () => import('./pages/account/login').then((m) => m.LoginPage),
    title: 'Anmelden | Hyrox-Shop',
  },
  {
    path: 'registrieren',
    loadComponent: () => import('./pages/account/register').then((m) => m.RegisterPage),
    title: 'Konto anlegen | Hyrox-Shop',
  },
  {
    path: 'bestellungen',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/orders/my-orders').then((m) => m.MyOrdersPage),
    title: 'Meine Bestellungen | Hyrox-Shop',
  },
  {
    path: 'bestellungen/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/orders/order-detail').then((m) => m.OrderDetailPage),
    title: 'Bestellung | Hyrox-Shop',
  },
  {
    path: 'retouren',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/orders/my-returns').then((m) => m.MyReturnsPage),
    title: 'Meine Retouren | Hyrox-Shop',
  },
  {
    path: 'konto',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/account/account-settings').then((m) => m.AccountSettingsPage),
    title: 'Mein Konto | Hyrox-Shop',
  },
  { path: 'pflege', pathMatch: 'full', redirectTo: 'pflege/produkte' },
  {
    path: 'pflege/:reiter',
    canActivate: [adminGuard],
    loadComponent: () => import('./pages/admin/admin').then((m) => m.AdminPage),
    title: 'Pflege | Hyrox-Shop',
  },
  {
    path: 'kein-zugriff',
    loadComponent: () => import('./pages/forbidden/forbidden').then((m) => m.ForbiddenPage),
    title: 'Kein Zugriff | Hyrox-Shop',
  },
  { path: '**', component: NotFoundPage, title: 'Seite nicht gefunden | Hyrox-Shop' },
];
