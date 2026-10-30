package de.hsaa.shop.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Eingabe beim Registrieren (docs/anforderungen/0006). */
public record RegisterRequest(
		@NotBlank(message = "Bitte deinen Namen eingeben.")
		@Size(min = 2, max = 100, message = "Der Name muss 2 bis 100 Zeichen lang sein.")
		String name,

		@NotBlank(message = "Bitte eine E-Mail-Adresse eingeben.")
		@Email(message = "Bitte eine gültige E-Mail-Adresse eingeben.")
		@Size(max = 254, message = "Die E-Mail-Adresse ist zu lang.")
		String email,

		@NotBlank(message = "Bitte ein Passwort eingeben.")
		@Size(min = 8, max = 72, message = "Das Passwort muss 8 bis 72 Zeichen lang sein.")
		String password) {
}
