import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductReturn, ReturnStatus } from '../api/models';

/** Gleiche Bezeichnungen wie im Backend (ReturnStatus.java). */
export const RETURN_STATUS_LABELS: Record<ReturnStatus, string> = {
  REQUESTED: 'Angemeldet',
  RECEIVED: 'Eingegangen und geprüft',
  REFUNDED: 'Erstattet',
  REJECTED: 'Abgelehnt',
};

const STEPS: ReturnStatus[] = ['REQUESTED', 'RECEIVED', 'REFUNDED'];

/**
 * Eine Retoure mit Fortschritt (Angemeldet → Eingegangen → Erstattet), damit Kunden jederzeit sehen,
 * wie weit sie ist (F1). Wird auf der Bestellseite, unter "Meine Retouren" und in der Pflege benutzt.
 */
@Component({
  selector: 'app-return-card',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  template: `
    @let r = productReturn();
    <article class="return" [class.rejected]="r.status === 'REJECTED'">
      <header>
        <strong>Retoure #{{ r.id }}</strong>
        <span class="meta">
          zu <a [routerLink]="['/bestellungen', r.orderId]">Bestellung #{{ r.orderId }}</a
          >, angemeldet am
          {{ r.createdAt | date: 'dd.MM.yyyy' }}
          @if (showCustomer()) {
            von {{ r.customerName }}
          }
        </span>
      </header>

      @if (r.status === 'REJECTED') {
        <p class="state rejected-text">Abgelehnt</p>
      } @else {
        <ol class="steps" aria-label="Stand der Retoure">
          @for (step of steps; track step; let i = $index) {
            <li
              [class.done]="i <= stepIndex()"
              [attr.aria-current]="i === stepIndex() ? 'step' : null"
            >
              {{ labels[step] }}
            </li>
          }
        </ol>
      }

      <ul class="lines">
        @for (line of r.lines; track line.productId) {
          <li>
            {{ line.quantity }} × {{ line.productName }} ({{ line.unitPrice | currency }})
            @if (line.condition === 'UNUSED') {
              <span class="tag ok">originalverschlossen</span>
            } @else if (line.condition === 'USED') {
              <span class="tag">geöffnet</span>
            }
          </li>
        }
      </ul>
      <p class="reason">Grund: {{ r.reason }}</p>
      @if (r.note) {
        <p class="note">Hinweis vom Shop: {{ r.note }}</p>
      }
      <p class="refund">
        @if (r.refundAmount !== null) {
          Erstattet: <strong>{{ r.refundAmount | currency }}</strong>
        } @else if (r.status !== 'REJECTED') {
          Voraussichtliche Erstattung: <strong>{{ r.expectedRefund | currency }}</strong>
        }
      </p>
      <ng-content />
    </article>
  `,
  styles: `
    .return {
      padding: 1rem 1.25rem;
      border: 1px solid var(--shop-border);
      border-radius: 1rem;
      background: var(--shop-card);
    }
    header {
      display: flex;
      flex-wrap: wrap;
      gap: 0.25rem 0.75rem;
      align-items: baseline;
    }
    .meta {
      color: var(--shop-muted);
      font-size: 0.9rem;
    }
    .steps {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin: 0.75rem 0;
      padding: 0;
      list-style: none;
      counter-reset: step;
    }
    .steps li {
      padding: 0.25rem 0.75rem;
      border-radius: 999px;
      background: #e9e6dd;
      color: var(--shop-muted);
      font-size: 0.85rem;
      font-weight: 700;
    }
    .steps li.done {
      background: #d9f2df;
      color: #1d6b35;
    }
    .rejected-text {
      color: var(--stock-out);
      font-weight: 700;
    }
    .lines {
      margin: 0.5rem 0;
      padding-left: 1.25rem;
    }
    .tag {
      margin-left: 0.4rem;
      padding: 0 0.4rem;
      border-radius: 999px;
      background: #f7dcda;
      font-size: 0.8rem;
    }
    .tag.ok {
      background: #d9f2df;
    }
    .reason,
    .note,
    .refund {
      margin: 0.25rem 0;
    }
    .note {
      font-style: italic;
    }
  `,
})
export class ReturnCard {
  readonly productReturn = input.required<ProductReturn>();
  readonly showCustomer = input(false);

  protected readonly labels = RETURN_STATUS_LABELS;
  protected readonly steps = STEPS;
  protected readonly stepIndex = computed(() => STEPS.indexOf(this.productReturn().status));
}
