package de.hsaa.shop.product;

import java.util.Arrays;
import java.util.stream.Collectors;

import de.hsaa.shop.common.InvalidFieldException;

/**
 * Darreichungsform eines Produkts (docs/anforderungen/0019). Unabhängig von der Kategorie:
 * "Protein" gibt es z. B. als Pulver, Riegel und Getränk. Danach lässt sich die Liste filtern.
 */
public enum ProductForm {

	PULVER("Pulver"),
	RIEGEL("Riegel"),
	GETRAENK("Getränk"),
	KAPSELN("Kapseln & Tabletten"),
	GEL("Gel");

	private final String label;

	ProductForm(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	/** Wert aus der URL (z. B. {@code form=RIEGEL}); leer = kein Filter, Unbekanntes = Feldfehler (400). */
	static ProductForm fromParam(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return Arrays.stream(values())
				.filter(form -> form.name().equalsIgnoreCase(value.trim()))
				.findFirst()
				.orElseThrow(() -> new InvalidFieldException("form", "Unbekannte Produktart. Erlaubt: "
						+ Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", ")) + "."));
	}
}
