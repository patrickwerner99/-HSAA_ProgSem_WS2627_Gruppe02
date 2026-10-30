import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable, map } from 'rxjs';
import { Category, Product, ProductInput } from '../../api/models';
import { ProductService } from '../../api/product.service';
import { fieldErrors, userMessage } from '../../shared/errors';
import { FallbackImageDirective } from '../../shared/fallback-image.directive';
import { IMAGE_URL_PATTERN, parsePrice, parseStock, priceForInput } from '../../shared/format';
import { NotifyService } from '../../shared/notify.service';
import { messageFor, notBlank, priceValidator, stockValidator } from './validators';

export interface ProductFormData {
  /** null = neues Produkt anlegen */
  product: Product | null;
  categories: Category[];
}

/**
 * Formular zum Anlegen und zum Ändern des ganzen Produkts (POST bzw. PUT).
 * Fehler erscheinen unter dem Feld, sobald man es verlässt; Fehler vom Backend landen
 * am passenden Feld. Bei einem Fehler bleibt der Dialog offen und die Eingaben bleiben erhalten.
 */
@Component({
  selector: 'app-product-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    FallbackImageDirective,
  ],
  templateUrl: './product-form-dialog.html',
  styleUrl: './form-dialog.css',
})
export class ProductFormDialog {
  private readonly productService = inject(ProductService);
  private readonly notify = inject(NotifyService);
  private readonly dialogRef = inject<MatDialogRef<ProductFormDialog, Product>>(MatDialogRef);
  protected readonly data = inject<ProductFormData>(MAT_DIALOG_DATA);

  protected readonly isNew = this.data.product === null;
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly messageFor = messageFor;

  protected readonly form = new FormGroup({
    name: new FormControl(this.data.product?.name ?? '', {
      nonNullable: true,
      validators: [
        Validators.required,
        notBlank,
        Validators.minLength(2),
        Validators.maxLength(100),
      ],
    }),
    categoryId: new FormControl<number | null>(this.data.product?.categoryId ?? null, {
      validators: [Validators.required],
    }),
    price: new FormControl(this.data.product ? priceForInput(this.data.product.price) : '', {
      nonNullable: true,
      validators: [Validators.required, priceValidator],
    }),
    stock: new FormControl(this.data.product ? String(this.data.product.stock) : '', {
      nonNullable: true,
      validators: [Validators.required, stockValidator],
    }),
    description: new FormControl(this.data.product?.description ?? '', {
      nonNullable: true,
      validators: [Validators.maxLength(1000)],
    }),
    imageUrl: new FormControl(this.data.product?.imageUrl ?? '', {
      nonNullable: true,
      validators: [Validators.pattern(IMAGE_URL_PATTERN), Validators.maxLength(500)],
    }),
  });

  /** Vorschau, sobald eine gültige Bild-Adresse eingetragen ist. */
  protected readonly preview = toSignal(
    this.form.controls.imageUrl.valueChanges.pipe(map((url) => this.previewUrl(url))),
    { initialValue: this.previewUrl(this.form.controls.imageUrl.value) },
  );

  protected save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const input: ProductInput = {
      name: value.name.trim(),
      categoryId: value.categoryId!,
      price: parsePrice(value.price)!,
      stock: parseStock(value.stock)!,
      description: value.description.trim() || null,
      imageUrl: value.imageUrl.trim() || null,
    };

    const product = this.data.product;
    const request$: Observable<Product> = product
      ? this.productService.replace(product.id, input)
      : this.productService.create(input);

    this.saving.set(true);
    this.formError.set(null);
    this.dialogRef.disableClose = true;

    request$.subscribe({
      next: (saved) => {
        this.notify.success('Produkt gespeichert');
        this.dialogRef.close(saved);
      },
      error: (error) => {
        this.saving.set(false);
        this.dialogRef.disableClose = false;
        const errors = fieldErrors(error);
        for (const { field, message } of errors) {
          const control = this.form.get(field);
          control?.setErrors({ server: message });
          control?.markAsTouched();
        }
        this.formError.set(
          errors.length > 0
            ? 'Bitte prüfe die markierten Felder.'
            : `Speichern fehlgeschlagen. ${userMessage(error)}`,
        );
      },
    });
  }

  private previewUrl(url: string): string | null {
    const trimmed = url.trim();
    return trimmed && IMAGE_URL_PATTERN.test(trimmed) ? trimmed : null;
  }
}
