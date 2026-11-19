import { Component, input } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AddressField, AddressForm, addressMessage } from './address-form';

/** Die vier Adressfelder als Baustein, damit Warenkorb und Konto gleich aussehen. */
@Component({
  selector: 'app-address-fields',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule],
  template: `
    <div class="address-fields" [formGroup]="form()">
      <mat-form-field appearance="outline" subscriptSizing="dynamic" class="wide">
        <mat-label>Name des Empfängers</mat-label>
        <input matInput formControlName="name" autocomplete="name" required />
        <mat-error>{{ message('name') }}</mat-error>
      </mat-form-field>
      <mat-form-field appearance="outline" subscriptSizing="dynamic" class="wide">
        <mat-label>Straße und Hausnummer</mat-label>
        <input matInput formControlName="street" autocomplete="street-address" required />
        <mat-error>{{ message('street') }}</mat-error>
      </mat-form-field>
      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>PLZ</mat-label>
        <input
          matInput
          formControlName="postalCode"
          autocomplete="postal-code"
          inputmode="numeric"
          maxlength="5"
          required
        />
        <mat-error>{{ message('postalCode') }}</mat-error>
      </mat-form-field>
      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Ort</mat-label>
        <input matInput formControlName="city" autocomplete="address-level2" required />
        <mat-error>{{ message('city') }}</mat-error>
      </mat-form-field>
    </div>
  `,
  styles: `
    .address-fields {
      display: grid;
      grid-template-columns: 8rem minmax(0, 1fr);
      gap: 0.75rem 1rem;
      max-width: 36rem;
    }
    .wide {
      grid-column: 1 / -1;
    }
    @media (max-width: 640px) {
      .address-fields {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class AddressFields {
  readonly form = input.required<AddressForm>();

  protected message(field: AddressField): string {
    return addressMessage(this.form(), field);
  }
}
