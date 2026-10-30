package de.hsaa.shop.account;

/** Rollen im Shop (docs/anforderungen/0006, 0007). */
public enum Role {
	/** Kann bestellen und eigene Bestellungen sehen. */
	CUSTOMER,
	/** Pflegt Produkte, Kategorien, Bilder und Bestellungen. */
	ADMIN;

	/** Name, den Spring Security erwartet, z. B. "ROLE_ADMIN". */
	public String authority() {
		return "ROLE_" + name();
	}
}
