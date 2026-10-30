import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CategoryService } from '../../api/category.service';
import { Category } from '../../api/models';
import { fieldErrors, userMessage } from '../../shared/errors';
import { NotifyService } from '../../shared/notify.service';
import { messageFor, notBlank } from './validators';

/** Kategorie anlegen oder umbenennen. Doppelte Namen meldet das Backend (409) am Feld. */
@Component({
  selector: 'app-category-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ category ? 'Kategorie umbenennen' : 'Neue Kategorie' }}</h2>
    <form (submit)="$event.preventDefault(); save()" novalidate>
      <mat-dialog-content class="fields">
        @if (formError(); as message) {
          <p class="form-error" role="alert">{{ message }}</p>
        }
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Name</mat-label>
          <input matInput [formControl]="name" required />
          <mat-error>{{ messageFor('categoryName', name) }}</mat-error>
        </mat-form-field>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close [disabled]="saving()">Abbrechen</button>
        <button mat-flat-button type="submit" [disabled]="name.invalid || saving()">
          Speichern
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styleUrl: './form-dialog.css',
})
export class CategoryFormDialog {
  private readonly categoryService = inject(CategoryService);
  private readonly notify = inject(NotifyService);
  private readonly dialogRef = inject<MatDialogRef<CategoryFormDialog, Category>>(MatDialogRef);
  protected readonly category = inject<Category | null>(MAT_DIALOG_DATA);

  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly messageFor = messageFor;
  protected readonly name = new FormControl(this.category?.name ?? '', {
    nonNullable: true,
    validators: [Validators.required, notBlank, Validators.minLength(2), Validators.maxLength(50)],
  });

  protected save(): void {
    if (this.name.invalid || this.saving()) {
      this.name.markAsTouched();
      return;
    }
    const name = this.name.value.trim();
    const request$ = this.category
      ? this.categoryService.rename(this.category.id, name)
      : this.categoryService.create(name);

    this.saving.set(true);
    this.formError.set(null);
    request$.subscribe({
      next: (saved) => {
        this.notify.success('Kategorie gespeichert');
        this.dialogRef.close(saved);
      },
      error: (error) => {
        this.saving.set(false);
        const [fieldError] = fieldErrors(error);
        if (fieldError || (error instanceof HttpErrorResponse && error.status === 409)) {
          // z. B. "Diese Kategorie gibt es schon." direkt am Feld
          this.name.setErrors({ server: fieldError?.message ?? userMessage(error) });
          this.name.markAsTouched();
        } else {
          this.formError.set(`Speichern fehlgeschlagen. ${userMessage(error)}`);
        }
      },
    });
  }
}
