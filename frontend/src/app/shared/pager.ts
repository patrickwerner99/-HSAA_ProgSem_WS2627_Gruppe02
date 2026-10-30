import { Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

/** Blättern für Tabellen (Bestellungen). Die Seite steht in der URL (?seite=2), siehe C2. */
@Component({
  selector: 'app-pager',
  imports: [MatButtonModule],
  template: `
    @if (pages() > 1) {
      <nav class="pager" aria-label="Seiten">
        <button
          mat-stroked-button
          type="button"
          [disabled]="page() === 0"
          (click)="go.emit(page() - 1)"
        >
          ← Zurück
        </button>
        <span>Seite {{ page() + 1 }} von {{ pages() }}</span>
        <button
          mat-stroked-button
          type="button"
          [disabled]="page() + 1 >= pages()"
          (click)="go.emit(page() + 1)"
        >
          Weiter →
        </button>
      </nav>
    }
  `,
  styles: `
    .pager {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 1rem;
      margin-top: 1.25rem;
    }
  `,
})
export class Pager {
  /** Aktuelle Seite, ab 0. */
  readonly page = input.required<number>();
  readonly totalItems = input.required<number>();
  readonly size = input.required<number>();
  readonly go = output<number>();

  protected readonly pages = computed(() =>
    Math.ceil(this.totalItems() / Math.max(this.size(), 1)),
  );
}
