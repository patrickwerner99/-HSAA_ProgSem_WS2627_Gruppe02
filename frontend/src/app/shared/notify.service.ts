import { Injectable, inject } from '@angular/core';
import { MatSnackBar, MatSnackBarRef, TextOnlySnackBar } from '@angular/material/snack-bar';

/** Einheitliche Rückmeldung unten am Bildschirm, für Erfolg und Fehler (0005). */
@Injectable({ providedIn: 'root' })
export class NotifyService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string, action = 'OK'): MatSnackBarRef<TextOnlySnackBar> {
    return this.snackBar.open(message, action, { duration: 4000, panelClass: 'snack-success' });
  }

  error(message: string): void {
    this.snackBar.open(message, 'Schließen', { duration: 6000, panelClass: 'snack-error' });
  }
}
