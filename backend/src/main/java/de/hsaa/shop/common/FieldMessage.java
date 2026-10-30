package de.hsaa.shop.common;

/** Ein Fehler zu einem Eingabefeld, z. B. {"field": "price", "message": "Der Preis muss größer als 0 sein."}. */
public record FieldMessage(String field, String message) {
}
