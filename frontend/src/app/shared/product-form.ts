import { Product, ProductForm } from '../api/models';

/**
 * Produktarten für Filter und Pflegeformular (docs/anforderungen/0019), gleiche Werte wie
 * ProductForm.java. "slug" steht in der URL (?art=riegel), damit Links lesbar bleiben (C2).
 */
export const PRODUCT_FORMS: { value: ProductForm; label: string; plural: string; slug: string }[] =
  [
    { value: 'PULVER', label: 'Pulver', plural: 'Pulver', slug: 'pulver' },
    { value: 'RIEGEL', label: 'Riegel', plural: 'Riegel', slug: 'riegel' },
    { value: 'GETRAENK', label: 'Getränk', plural: 'Getränke', slug: 'getraenke' },
    {
      value: 'KAPSELN',
      label: 'Kapseln & Tabletten',
      plural: 'Kapseln & Tabletten',
      slug: 'kapseln',
    },
    { value: 'GEL', label: 'Gel', plural: 'Gels', slug: 'gels' },
  ];

/** ?art=riegel → RIEGEL; Unbekanntes → null (= alle), damit ein kaputter Link nicht zu einem Fehler führt. */
export function formFromUrl(slug: string | null): ProductForm | null {
  return PRODUCT_FORMS.find((option) => option.slug === slug)?.value ?? null;
}

export function formSlug(form: ProductForm | null): string | null {
  return PRODUCT_FORMS.find((option) => option.value === form)?.slug ?? null;
}

export function formLabel(form: ProductForm | null | undefined): string {
  return PRODUCT_FORMS.find((option) => option.value === form)?.label ?? '';
}

/** "Whey Protein Schoko (1 kg)" – so heißt das Produkt auch in der Bestellung. */
export function displayName(product: Pick<Product, 'name' | 'size'>): string {
  return product.size ? `${product.name} (${product.size})` : product.name;
}
