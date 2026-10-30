import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './api/auth.service';
import { CartService } from './shared/cart.service';
import { NotifyService } from './shared/notify.service';

/** Rahmen der Anwendung: Kopfzeile mit Navigation, Warenkorb und Konto, darunter die aktuelle Seite. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly cart = inject(CartService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotifyService);

  protected logout(): void {
    this.auth.logout().subscribe(() => {
      this.notify.success('Du bist abgemeldet.');
      this.router.navigateByUrl('/');
    });
  }
}
