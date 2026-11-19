import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { ProductReturn } from '../../api/models';
import { ReturnService } from '../../api/return.service';
import { userMessage } from '../../shared/errors';
import { ReturnCard } from '../../shared/return-card';

/** /retouren – alle eigenen Retouren mit Stand (Sprint 3, F1: jederzeit sehen, wie weit sie ist). */
@Component({
  selector: 'app-my-returns',
  imports: [RouterLink, MatButtonModule, MatProgressSpinnerModule, ReturnCard],
  template: `
    <h1>Meine Retouren</h1>
    @if (loading()) {
      <div class="state-message"><mat-spinner diameter="36" /></div>
    } @else if (error(); as message) {
      <div class="state-message error" role="alert">
        <p>{{ message }}</p>
        <button mat-flat-button type="button" (click)="load()">Erneut versuchen</button>
      </div>
    } @else if (returns().length === 0) {
      <div class="state-message">
        <p>Du hast noch nichts zurückgeschickt.</p>
        <p class="hint">
          Retouren meldest du in einer zugestellten Bestellung an, unter
          <a routerLink="/bestellungen">Meine Bestellungen</a>.
        </p>
      </div>
    } @else {
      <div class="list">
        @for (r of returns(); track r.id) {
          <app-return-card [productReturn]="r" />
        }
      </div>
    }
  `,
  styles: `
    .list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
  `,
})
export class MyReturnsPage {
  private readonly service = inject(ReturnService);

  protected readonly returns = signal<ProductReturn[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.list({ page: 0 }).subscribe({
      next: (page) => {
        this.returns.set(page.items);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.error.set(userMessage(error));
      },
    });
  }
}
