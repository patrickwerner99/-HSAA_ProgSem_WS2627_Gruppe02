package de.hsaa.shop.discount;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Ein- und Ausgabeformate für Rabattcodes (G2). */
public final class DiscountDtos {

	private DiscountDtos() {
	}

	/** Code anlegen (Admin). 1 bis 50 %, damit niemand aus Versehen alles verschenkt. */
	public record DiscountCodeRequest(
			@NotBlank(message = "Bitte einen Code eingeben.")
			@Pattern(regexp = "[A-Za-z0-9-]{3,20}", message = "Der Code hat 3 bis 20 Zeichen: Buchstaben, Ziffern und -.")
			String code,

			@NotNull(message = "Bitte einen Rabatt in Prozent eingeben.")
			@Min(value = 1, message = "Der Rabatt muss zwischen 1 und 50 % liegen.")
			@Max(value = 50, message = "Der Rabatt muss zwischen 1 und 50 % liegen.")
			Integer percent,

			@NotNull(message = "Bitte angeben, ab wann der Code gilt.")
			LocalDate validFrom,

			@NotNull(message = "Bitte angeben, bis wann der Code gilt.")
			LocalDate validUntil,

			@Min(value = 1, message = "Mindestens 1 Einlösung, oder leer lassen für unbegrenzt.")
			Integer maxUses) {
	}

	/** Code ändern (Admin): abschalten bzw. wieder einschalten oder verlängern. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record DiscountCodePatch(Boolean active, LocalDate validUntil) {
	}

	/** Für den Admin: alles, auch wie oft der Code schon benutzt wurde. */
	public record DiscountCodeResponse(Long id, String code, int percent, LocalDate validFrom, LocalDate validUntil,
			Integer maxUses, int timesUsed, boolean active, boolean usableToday) {

		static DiscountCodeResponse from(DiscountCode code, LocalDate today) {
			return new DiscountCodeResponse(code.getId(), code.getCode(), code.getPercent(), code.getValidFrom(),
					code.getValidUntil(), code.getMaxUses(), code.getTimesUsed(), code.isActive(),
					code.problem(today) == null);
		}
	}

	/** Für Kunden beim Einlösen: nur, was sie wissen müssen. */
	public record DiscountCheckResponse(String code, int percent, LocalDate validUntil) {
	}
}
