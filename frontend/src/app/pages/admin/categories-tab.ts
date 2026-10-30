import { Component, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { CategoryService } from '../../api/category.service';
import { Category } from '../../api/models';
import { ConfirmDialog, ConfirmData } from '../../shared/confirm-dialog';
import { userMessage } from '../../shared/errors';
import { NotifyService } from '../../shared/notify.service';
import { CategoryFormDialog } from './category-form-dialog';

/** Reiter "Kategorien": anlegen, umbenennen, löschen (nur wenn leer). */
@Component({
  selector: 'app-categories-tab',
  imports: [MatButtonModule],
  template: `
    <div class="toolbar">
      <p class="hint">Kategorien mit Produkten lassen sich erst löschen, wenn sie leer sind.</p>
      <button mat-flat-button type="button" (click)="openForm(null)">+ Neue Kategorie</button>
    </div>

    @if (categories().length === 0) {
      <div class="state-message"><p>Noch keine Kategorien vorhanden.</p></div>
    } @else {
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th scope="col">Name</th>
              <th scope="col" class="num">Produkte</th>
              <th scope="col"><span class="visually-hidden">Aktionen</span></th>
            </tr>
          </thead>
          <tbody>
            @for (category of categories(); track category.id) {
              <tr>
                <td class="name">{{ category.name }}</td>
                <td class="num">{{ category.productCount }}</td>
                <td class="actions">
                  <button mat-stroked-button type="button" (click)="openForm(category)">
                    Umbenennen
                  </button>
                  <button mat-button type="button" class="delete" (click)="delete(category)">
                    Löschen
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
  styleUrl: './admin-table.css',
})
export class CategoriesTab {
  private readonly categoryService = inject(CategoryService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);

  readonly categories = input.required<Category[]>();
  readonly changed = output<void>();

  protected openForm(category: Category | null): void {
    this.dialog
      .open(CategoryFormDialog, { data: category, width: '28rem', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.changed.emit();
        }
      });
  }

  protected delete(category: Category): void {
    // Gar nicht erst fragen, wenn es ohnehin nicht geht. Das Backend prüft trotzdem (409).
    if (category.productCount > 0) {
      const produkte =
        category.productCount === 1 ? '1 Produkt' : `${category.productCount} Produkte`;
      this.notify.error(
        `Die Kategorie enthält noch ${produkte}. Bitte die Produkte zuerst verschieben oder löschen.`,
      );
      return;
    }
    const data: ConfirmData = {
      title: 'Kategorie löschen?',
      message: `Möchtest du die Kategorie „${category.name}“ wirklich löschen?`,
      confirmLabel: 'Löschen',
    };
    this.dialog
      .open(ConfirmDialog, { data })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.categoryService.delete(category.id).subscribe({
          next: () => {
            this.notify.success(`Kategorie „${category.name}“ gelöscht`);
            this.changed.emit();
          },
          error: (error) => this.notify.error(userMessage(error)),
        });
      });
  }
}
