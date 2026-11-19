package de.hsaa.shop.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.hsaa.shop.common.AddressRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Ein- und Ausgabeformate für das eigene Konto (docs/anforderungen/0021, 0022). */
public final class AccountDtos {

	private AccountDtos() {
	}

	/**
	 * Kontodaten ändern (E1). Fehlende Felder bleiben, wie sie sind. Wer die E-Mail ändert, muss das
	 * aktuelle Passwort mitschicken, damit niemand an einem offenen Rechner das Konto übernimmt.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record AccountUpdateRequest(
			@Size(min = 2, max = 100, message = "Der Name muss 2 bis 100 Zeichen lang sein.")
			@Pattern(regexp = "(?s).*\\S.*", message = "Bitte deinen Namen eingeben.")
			String name,

			@Email(message = "Bitte eine gültige E-Mail-Adresse eingeben.")
			@Size(max = 254, message = "Die E-Mail-Adresse ist zu lang.")
			@Pattern(regexp = "(?s).*\\S.*", message = "Bitte eine E-Mail-Adresse eingeben.")
			String email,

			String currentPassword) {
	}

	/** Passwort ändern (E1): aktuelles Passwort zur Bestätigung, neues mit denselben Regeln wie beim Registrieren. */
	public record PasswordChangeRequest(
			@NotBlank(message = "Bitte gib dein aktuelles Passwort ein.")
			String currentPassword,

			@NotBlank(message = "Bitte ein neues Passwort eingeben.")
			@Size(min = 8, max = 72, message = "Das Passwort muss 8 bis 72 Zeichen lang sein.")
			String newPassword) {
	}

	/** Adresse anlegen oder ändern (E2). Gleiche Regeln wie die Lieferadresse beim Bestellen. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record AddressRequest(
			@Size(max = 30, message = "Die Bezeichnung darf höchstens 30 Zeichen lang sein.")
			String label,

			@NotBlank(message = AddressRules.NAME_REQUIRED)
			@Size(min = 2, max = 100, message = AddressRules.NAME_SIZE)
			String name,

			@NotBlank(message = AddressRules.STREET_REQUIRED)
			@Size(max = 100, message = AddressRules.STREET_SIZE)
			@Pattern(regexp = AddressRules.STREET_PATTERN, message = AddressRules.STREET_NUMBER)
			String street,

			@NotBlank(message = AddressRules.POSTAL_CODE_REQUIRED)
			@Pattern(regexp = AddressRules.POSTAL_CODE_PATTERN, message = AddressRules.POSTAL_CODE_FORMAT)
			String postalCode,

			@NotBlank(message = AddressRules.CITY_REQUIRED)
			@Size(min = 2, max = 60, message = AddressRules.CITY_SIZE)
			String city,

			/** true = diese Adresse wird die Standardadresse. */
			Boolean standard) {
	}

	public record AddressResponse(Long id, String label, String name, String street, String postalCode, String city,
			boolean standard) {

		static AddressResponse from(SavedAddress address) {
			return new AddressResponse(address.getId(), address.getLabel(), address.getName(), address.getStreet(),
					address.getPostalCode(), address.getCity(), address.isStandard());
		}
	}
}
