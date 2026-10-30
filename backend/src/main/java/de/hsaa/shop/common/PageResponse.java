package de.hsaa.shop.common;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Eine Seite einer Liste (Produkte, Bestellungen). Gleiches Format für alle Listen-Endpunkte.
 * {@code last} sagt dem Frontend, dass nichts mehr nachzuladen ist (ADR-0002).
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, boolean last) {

	public static <E, T> PageResponse<T> of(Page<E> page, java.util.function.Function<E, T> mapper) {
		return new PageResponse<>(
				page.getContent().stream().map(mapper).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.isLast());
	}
}
