package de.hsaa.shop.category;

/** Antwort der API für eine Kategorie, inklusive Anzahl der Produkte darin. */
public record CategoryResponse(Long id, String name, long productCount) {
}
