import { TestBed } from '@angular/core/testing';
import { CartService, MAX_QUANTITY } from './cart.service';

const KEY = 'hyrox-shop-warenkorb';

describe('CartService', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  const cart = () => TestBed.inject(CartService);

  it('zählt gleiche Produkte zusammen und speichert nur ID und Menge', () => {
    const service = cart();
    service.add(7, 2);
    service.add(7, 3);
    service.add(9, 1);

    expect(service.quantityOf(7)).toBe(5);
    expect(service.count()).toBe(6);
    const saved = JSON.parse(localStorage.getItem(KEY)!);
    expect(saved).toContainEqual({ productId: 7, quantity: 5 });
    expect(saved).toContainEqual({ productId: 9, quantity: 1 });
  });

  it('nimmt nie mehr als 99 Stück an', () => {
    const service = cart();
    service.add(7, 500);
    expect(service.quantityOf(7)).toBe(MAX_QUANTITY);
  });

  it('entfernt ein Produkt bei Menge 0', () => {
    const service = cart();
    service.add(7, 2);
    service.set(7, 0);
    expect(service.items()).toEqual([]);
  });

  it('übersteht Neuladen: liest den gespeicherten Warenkorb', () => {
    localStorage.setItem(KEY, JSON.stringify([{ productId: 3, quantity: 2 }]));
    expect(cart().count()).toBe(2);
  });

  it('startet bei kaputtem Speicherinhalt mit leerem Warenkorb', () => {
    localStorage.setItem(KEY, '{kein json');
    expect(cart().items()).toEqual([]);
  });
});
