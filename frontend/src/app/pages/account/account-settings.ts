import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AccountService } from '../../api/account.service';
import { AuthService } from '../../api/auth.service';
import { SavedAddress } from '../../api/models';
import { AddressFields } from '../../shared/address-fields';
import { addressValue, applyAddressErrors, createAddressForm } from '../../shared/address-form';
import { fieldErrors, userMessage } from '../../shared/errors';
import { NotifyService } from '../../shared/notify.service';
import { notBlank } from '../admin/validators';

/**
 * /konto – Kontodaten und gespeicherte Adressen selbst pflegen (Sprint 3, E1/E2,
 * docs/anforderungen/0021, 0022). Alle Regeln prüft verbindlich das Backend.
 */
@Component({
  selector: 'app-account-settings',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    AddressFields,
  ],
  templateUrl: './account-settings.html',
  styleUrl: './account-settings.css',
})
export class AccountSettingsPage {
  private readonly account = inject(AccountService);
  private readonly auth = inject(AuthService);
  private readonly notify = inject(NotifyService);

  // ---- E1: Kontodaten ----
  protected readonly profile = new FormGroup({
    name: new FormControl(this.auth.user()?.name ?? '', {
      nonNullable: true,
      validators: [
        Validators.required,
        notBlank,
        Validators.minLength(2),
        Validators.maxLength(100),
      ],
    }),
    email: new FormControl(this.auth.user()?.email ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(254)],
    }),
    currentPassword: new FormControl('', { nonNullable: true }),
  });
  protected readonly emailChanged = signal(false);
  protected readonly profileError = signal<string | null>(null);
  protected readonly savingProfile = signal(false);

  protected readonly password = new FormGroup({
    currentPassword: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    newPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8), Validators.maxLength(72)],
    }),
  });
  protected readonly passwordError = signal<string | null>(null);
  protected readonly savingPassword = signal(false);

  // ---- E2: Adressen ----
  protected readonly addresses = signal<SavedAddress[]>([]);
  protected readonly addressesLoading = signal(true);
  protected readonly addressesError = signal<string | null>(null);
  /** null = kein Formular offen, 'neu' = neue Adresse, sonst die ID der bearbeiteten Adresse. */
  protected readonly editing = signal<number | 'neu' | null>(null);
  protected readonly addressForm = createAddressForm();
  protected readonly label = new FormControl('', {
    nonNullable: true,
    validators: [Validators.maxLength(30)],
  });
  protected readonly makeStandard = new FormControl(false, { nonNullable: true });
  protected readonly addressError = signal<string | null>(null);
  protected readonly savingAddress = signal(false);
  protected readonly canAddMore = computed(() => this.addresses().length < 10);

  constructor() {
    this.profile.controls.email.valueChanges.subscribe((value) =>
      this.emailChanged.set(value.trim().toLowerCase() !== this.auth.user()?.email),
    );
    this.loadAddresses();
  }

  protected saveProfile(): void {
    if (this.profile.invalid || this.savingProfile()) {
      this.profile.markAllAsTouched();
      return;
    }
    const { name, email, currentPassword } = this.profile.getRawValue();
    this.savingProfile.set(true);
    this.profileError.set(null);
    this.account
      .update({
        name: name.trim(),
        ...(this.emailChanged() ? { email: email.trim(), currentPassword } : {}),
      })
      .subscribe({
        next: (user) => {
          this.savingProfile.set(false);
          this.auth.updateUser(user);
          this.profile.controls.currentPassword.setValue('');
          this.emailChanged.set(false);
          this.notify.success('Kontodaten gespeichert');
        },
        error: (error) => {
          this.savingProfile.set(false);
          const errors = fieldErrors(error);
          for (const { field, message } of errors) {
            this.profile.get(field)?.setErrors({ server: message });
            this.profile.get(field)?.markAsTouched();
          }
          this.profileError.set(
            errors.length > 0 ? 'Bitte prüfe die markierten Felder.' : userMessage(error),
          );
        },
      });
  }

  protected savePassword(): void {
    if (this.password.invalid || this.savingPassword()) {
      this.password.markAllAsTouched();
      return;
    }
    const { currentPassword, newPassword } = this.password.getRawValue();
    this.savingPassword.set(true);
    this.passwordError.set(null);
    this.account.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.savingPassword.set(false);
        this.password.reset();
        this.notify.success('Passwort geändert');
      },
      error: (error) => {
        this.savingPassword.set(false);
        const errors = fieldErrors(error);
        for (const { field, message } of errors) {
          this.password.get(field)?.setErrors({ server: message });
          this.password.get(field)?.markAsTouched();
        }
        this.passwordError.set(
          errors.length > 0 ? 'Bitte prüfe die markierten Felder.' : userMessage(error),
        );
      },
    });
  }

  protected fieldMessage(control: FormControl<string>, required: string, other: string): string {
    const errors = control.errors ?? {};
    if (typeof errors['server'] === 'string') {
      return errors['server'];
    }
    return errors['required'] || errors['blank'] ? required : other;
  }

  // ---- Adressen ----

  protected loadAddresses(): void {
    this.addressesLoading.set(true);
    this.addressesError.set(null);
    this.account.addresses().subscribe({
      next: (list) => {
        this.addresses.set(list);
        this.addressesLoading.set(false);
      },
      error: (error) => {
        this.addressesLoading.set(false);
        this.addressesError.set(userMessage(error));
      },
    });
  }

  protected startNew(): void {
    this.addressForm.reset();
    this.label.setValue('');
    this.makeStandard.setValue(this.addresses().length === 0);
    this.addressError.set(null);
    this.editing.set('neu');
  }

  protected startEdit(address: SavedAddress): void {
    this.addressForm.reset({
      name: address.name,
      street: address.street,
      postalCode: address.postalCode,
      city: address.city,
    });
    this.label.setValue(address.label ?? '');
    this.makeStandard.setValue(address.standard);
    this.addressError.set(null);
    this.editing.set(address.id);
  }

  protected cancelEdit(): void {
    this.editing.set(null);
  }

  protected saveAddress(): void {
    if (this.addressForm.invalid || this.label.invalid || this.savingAddress()) {
      this.addressForm.markAllAsTouched();
      return;
    }
    const input = {
      ...addressValue(this.addressForm),
      label: this.label.value.trim() || null,
      standard: this.makeStandard.value,
    };
    const editing = this.editing();
    const request$ =
      editing === 'neu' || editing === null
        ? this.account.addAddress(input)
        : this.account.updateAddress(editing, input);
    this.savingAddress.set(true);
    this.addressError.set(null);
    request$.subscribe({
      next: () => {
        this.savingAddress.set(false);
        this.editing.set(null);
        this.notify.success('Adresse gespeichert');
        this.loadAddresses();
      },
      error: (error) => {
        this.savingAddress.set(false);
        const errors = fieldErrors(error);
        applyAddressErrors(this.addressForm, errors);
        this.addressError.set(
          errors.length > 0 ? 'Bitte prüfe die markierten Felder.' : userMessage(error),
        );
      },
    });
  }

  protected setStandard(address: SavedAddress): void {
    this.account.updateAddress(address.id, { ...address, standard: true }).subscribe({
      next: () => this.loadAddresses(),
      error: (error) => this.addressesError.set(userMessage(error)),
    });
  }

  protected remove(address: SavedAddress): void {
    this.account.deleteAddress(address.id).subscribe({
      next: () => {
        this.notify.success('Adresse gelöscht');
        this.loadAddresses();
      },
      error: (error) => this.addressesError.set(userMessage(error)),
    });
  }
}
