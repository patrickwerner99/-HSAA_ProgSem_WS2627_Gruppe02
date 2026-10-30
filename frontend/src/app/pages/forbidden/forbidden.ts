import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

/** Angemeldet, aber kein Admin – z. B. wenn ein Kunde einen Link auf /pflege öffnet (A2). */
@Component({
  selector: 'app-forbidden',
  imports: [RouterLink, MatButtonModule],
  template: `
    <div class="state-message">
      <p>Diese Seite ist nur für Admins.</p>
      <a mat-flat-button routerLink="/">Zum Shop</a>
    </div>
  `,
})
export class ForbiddenPage {}
