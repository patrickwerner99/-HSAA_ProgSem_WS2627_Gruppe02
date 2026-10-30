import { Directive, ElementRef, inject } from '@angular/core';
import { PLACEHOLDER_IMAGE } from './format';

/**
 * Ersetzt ein Bild, das nicht lädt, durch den Platzhalter (statt des kaputten Bild-Symbols).
 * Verwendung: <img [src]="product.imageUrl || placeholder" appFallbackImage>
 */
@Directive({
  selector: 'img[appFallbackImage]',
  host: { '(error)': 'useFallback()' },
})
export class FallbackImageDirective {
  private readonly img = inject<ElementRef<HTMLImageElement>>(ElementRef).nativeElement;

  protected useFallback(): void {
    // Nur einmal ersetzen, sonst entsteht eine Endlosschleife, falls auch der Platzhalter fehlt.
    if (!this.img.src.endsWith(PLACEHOLDER_IMAGE)) {
      this.img.src = PLACEHOLDER_IMAGE;
    }
  }
}
