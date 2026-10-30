package de.hsaa.shop.account;

import jakarta.validation.constraints.NotBlank;

/** Eingabe beim Anmelden. */
public record LoginRequest(
		@NotBlank(message = "Bitte deine E-Mail-Adresse eingeben.") String email,
		@NotBlank(message = "Bitte dein Passwort eingeben.") String password) {
}
