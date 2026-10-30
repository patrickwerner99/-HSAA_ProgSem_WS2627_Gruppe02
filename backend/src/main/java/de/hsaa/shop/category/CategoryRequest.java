package de.hsaa.shop.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Eingabe beim Anlegen oder Umbenennen einer Kategorie. */
public record CategoryRequest(
		@NotBlank(message = "Bitte einen Namen eingeben.")
		@Size(min = 2, max = 50, message = "Der Name muss 2 bis 50 Zeichen lang sein.")
		String name) {
}
