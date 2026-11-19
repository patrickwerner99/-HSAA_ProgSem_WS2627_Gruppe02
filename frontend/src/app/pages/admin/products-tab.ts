import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { Category, Product, ProductInput } from '../../api/models';
import { ProductService } from '../../api/product.service';
import { ConfirmDialog, ConfirmData } from '../../shared/confirm-dialog';
import { fieldErrors, userMessage } from '../../shared/errors';
import { FallbackImageDirective } from '../../shared/fallback-image.directive';
import { PLACEHOLDER_IMAGE, parsePrice, parseStock, priceForInput } from '../../shared/format';
import { NotifyService } from '../../shared/notify.service';
import { ProductFormData, ProductFormDialog } from './product-form-dialog';

/**
 * Reiter "Produkte": ganzes Produkt über das Formular ändern, Preis und Bestand direkt
 * in der Tabelle (einzelne Felder, PATCH), Löschen mit Sicherheitsabfrage.
 */
@Component({
  selector: 'app-products-tab',
  imports: [CurrencyPipe, DatePipe, MatButtonModule, FallbackImageDirective],
  templateUrl: './products-tab.html',
  styleUrl: './admin-table.css',
})
export class ProductsTab {
  private readonly productService = inject(ProductService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);

  readonly products = input.required<Product[]>();
  readonly categories = input.required<Category[]>();
  /** Etwas wurde gespeichert oder gelöscht → Seite lädt die Daten neu. */
  readonly changed = output<void>();

  protected readonly placeholder = PLACEHOLDER_IMAGE;
  protected readonly priceForInput = priceForInput;

  protected openForm(product: Product | null): void {
    const data: ProductFormData = { product, categories: this.categories() };
    this.dialog
      .open(ProductFormDialog, { data, width: '40rem', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.changed.emit();
        }
      });
  }

  protected savePrice(product: Product, field: HTMLInputElement): void {
    const price = parsePrice(field.value);
    if (price === null || price <= 0 || price > 9999.99) {
      this.notify.error(
        'Der Preis muss größer als 0 sein und darf höchstens zwei Nachkommastellen haben.',
      );
      field.value = priceForInput(product.regularPrice);
      return;
    }
    if (price === product.regularPrice) {
      field.value = priceForInput(price);
      return;
    }
    this.saveField(product, { price }, `Preis von „${product.name}“ gespeichert`, () => {
      field.value = priceForInput(product.regularPrice);
    });
  }

  protected saveStock(product: Product, field: HTMLInputElement): void {
    const stock = parseStock(field.value);
    if (stock === null) {
      this.notify.error('Der Bestand muss eine ganze Zahl ab 0 sein.');
      field.value = String(product.stock);
      return;
    }
    if (stock === product.stock) {
      return;
    }
    this.saveField(product, { stock }, `Bestand von „${product.name}“ gespeichert`, () => {
      field.value = String(product.stock);
    });
  }

  protected confirmDelete(product: Product): void {
    const data: ConfirmData = {
      title: 'Produkt löschen?',
      message: `Möchtest du „${product.name}“ wirklich löschen? Das lässt sich nicht rückgängig machen.`,
      confirmLabel: 'Löschen',
    };
    this.dialog
      .open(ConfirmDialog, { data })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.productService.delete(product.id).subscribe({
          next: () => {
            this.notify.success(`„${product.name}“ gelöscht`);
            this.changed.emit();
          },
          error: (error) => this.notify.error(userMessage(error)),
        });
      });
  }

  /** Einzelnes Feld per PATCH speichern; bei Fehler den alten Wert wiederherstellen. */
  private saveField(
    product: Product,
    changes: Partial<ProductInput>,
    successMessage: string,
    revert: () => void,
  ): void {
    this.productService.patch(product.id, changes).subscribe({
      next: () => {
        this.notify.success(successMessage);
        this.changed.emit();
      },
      error: (error) => {
        revert();
        const [firstFieldError] = fieldErrors(error);
        this.notify.error(firstFieldError?.message ?? userMessage(error));
      },
    });
  }
}
