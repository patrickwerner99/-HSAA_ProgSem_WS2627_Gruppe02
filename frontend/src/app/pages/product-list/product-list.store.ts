import { Injectable, inject } from '@angular/core';
import { NavigationStart, Router } from '@angular/router';
import { filter } from 'rxjs';
import { Product } from '../../api/models';

export interface ListSnapshot {
  key: string;
  items: Product[];
  page: number;
  last: boolean;
  total: number;
  scrollY: number;
}

/**
 * Merkt sich die geladene Liste und die Scroll-Position, wenn man zur Detailseite wechselt.
 * Beim Zurück-Button wird beides wiederhergestellt, statt wieder oben anzufangen (0001).
 * Das ist der größte Nachteil von endlosem Scrollen, den wir damit abfangen (ADR-0002).
 */
@Injectable({ providedIn: 'root' })
export class ProductListStore {
  private snapshot: ListSnapshot | null = null;
  private lastTrigger: 'imperative' | 'popstate' | 'hashchange' = 'imperative';

  constructor() {
    inject(Router)
      .events.pipe(filter((event): event is NavigationStart => event instanceof NavigationStart))
      .subscribe((event) => (this.lastTrigger = event.navigationTrigger ?? 'imperative'));
  }

  save(snapshot: ListSnapshot): void {
    this.snapshot = snapshot;
  }

  /** Nur bei Zurück/Vor im Browser und gleichem Filter. Sonst frisch laden, damit Änderungen sichtbar sind. */
  restore(key: string): ListSnapshot | null {
    if (this.lastTrigger === 'popstate' && this.snapshot?.key === key) {
      return this.snapshot;
    }
    return null;
  }
}
