import { parsePrice, parseStock, priceForInput, stockView } from './format';

describe('parsePrice', () => {
  it('akzeptiert Komma und Punkt', () => {
    expect(parsePrice('34,90')).toBe(34.9);
    expect(parsePrice('34.90')).toBe(34.9);
    expect(parsePrice('34')).toBe(34);
  });

  it('lehnt ungültige Preise ab', () => {
    expect(parsePrice('')).toBeNull();
    expect(parsePrice('abc')).toBeNull();
    expect(parsePrice('-5')).toBeNull();
    expect(parsePrice('3,999')).toBeNull();
    expect(parsePrice('12345')).toBeNull();
  });
});

describe('priceForInput', () => {
  it('zeigt immer zwei Nachkommastellen mit Komma', () => {
    expect(priceForInput(34.9)).toBe('34,90');
    expect(priceForInput(5)).toBe('5,00');
  });
});

describe('parseStock', () => {
  it('akzeptiert ganze Zahlen ab 0', () => {
    expect(parseStock('0')).toBe(0);
    expect(parseStock('24')).toBe(24);
  });

  it('lehnt Kommazahlen und negative Werte ab', () => {
    expect(parseStock('3,5')).toBeNull();
    expect(parseStock('-1')).toBeNull();
    expect(parseStock('')).toBeNull();
  });
});

describe('stockView', () => {
  it('liefert Text und Farbe je Status', () => {
    expect(stockView('IN_STOCK').label).toBe('Auf Lager');
    expect(stockView('LOW_STOCK').label).toBe('Nur noch wenige verfügbar');
    expect(stockView('SOLD_OUT').label).toBe('Ausverkauft');
    expect(stockView('SOLD_OUT').cssClass).toBe('stock-out');
  });

  it('nennt bei wenig Bestand die genaue Stückzahl', () => {
    expect(stockView('LOW_STOCK', 3).label).toBe('Nur noch 3 verfügbar');
    expect(stockView('IN_STOCK', 40).label).toBe('Auf Lager');
  });
});
