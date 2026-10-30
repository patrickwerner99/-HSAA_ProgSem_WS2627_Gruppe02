import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

/** Unbekannte Adresse, z. B. /gibtsnicht (docs/anforderungen/0005). */
@Component({
  selector: 'app-not-found',
  imports: [RouterLink, MatButtonModule],
  template: `
    <div class="state-message">
      <p>Diese Seite gibt es nicht.</p>
      <a mat-flat-button routerLink="/">Zur Startseite</a>
    </div>
  `,
})
export class NotFoundPage {}
