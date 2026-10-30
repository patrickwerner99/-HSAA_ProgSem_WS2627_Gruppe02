import { Component, computed, input } from '@angular/core';
import { OrderStatus } from '../api/models';
import { ORDER_STATUS_LABELS } from './order-status';

/** Status einer Bestellung als farbiges Etikett; die Bedeutung steht immer im Text. */
@Component({
  selector: 'app-status-chip',
  template: `<span class="chip {{ status().toLowerCase() }}">{{ label() }}</span>`,
  styles: `
    .chip {
      display: inline-block;
      padding: 0.2rem 0.7rem;
      border-radius: 999px;
      font-size: 0.85rem;
      font-weight: 700;
      white-space: nowrap;
      background: #e9e6dd;
    }
    .received {
      background: #fff1c2;
    }
    .processing {
      background: #dce9ff;
    }
    .shipped {
      background: #e1dcff;
    }
    .delivered {
      background: #d9f2df;
      color: var(--stock-ok);
    }
    .cancelled {
      background: #f7dcda;
      color: var(--stock-out);
    }
  `,
})
export class StatusChip {
  readonly status = input.required<OrderStatus>();
  protected readonly label = computed(() => ORDER_STATUS_LABELS[this.status()]);
}
