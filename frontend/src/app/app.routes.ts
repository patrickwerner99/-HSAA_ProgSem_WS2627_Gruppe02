import { Routes } from '@angular/router';
import { NotFoundPage } from './pages/not-found/not-found';
import { ProductListPage } from './pages/product-list/product-list';

// Die Startseite wird sofort geladen, Detail- und Pflegeseite erst bei Bedarf (kleinerer Start-Download).
export const routes: Routes = [
  { path: '', component: ProductListPage, title: 'Hyrox-Shop' },
  {
    path: 'produkte/:id',
    loadComponent: () =>
      import('./pages/product-detail/product-detail').then((m) => m.ProductDetailPage),
    title: 'Produkt | Hyrox-Shop',
  },
  {
    path: 'pflege',
    loadComponent: () => import('./pages/admin/admin').then((m) => m.AdminPage),
    title: 'Pflege | Hyrox-Shop',
  },
  { path: '**', component: NotFoundPage, title: 'Seite nicht gefunden | Hyrox-Shop' },
];
