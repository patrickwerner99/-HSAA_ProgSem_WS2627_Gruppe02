package de.hsaa.shop.product;

import java.util.List;

/**
 * Eine Ladung Produkte für das endlose Scrollen (ADR-0002).
 * {@code last} sagt dem Frontend, dass nichts mehr nachzuladen ist.
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, boolean last) {
}
