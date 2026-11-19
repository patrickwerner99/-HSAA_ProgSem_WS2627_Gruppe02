package de.hsaa.shop.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Lieferadresse einer Bestellung (docs/anforderungen/0017, ADR-0009). Sie wird beim Bestellen in die
 * Bestellung kopiert, genau wie die Preise: Zieht der Kunde später um, bleibt die alte Bestellung richtig.
 * Geliefert wird nur innerhalb Deutschlands, deshalb gibt es kein Feld für das Land.
 * Die Spalten dürfen leer sein, weil Bestellungen aus Sprint 2 noch keine Adresse haben.
 */
@Embeddable
public class DeliveryAddress {

	@Column(name = "delivery_name", length = 100)
	private String name;

	@Column(name = "delivery_street", length = 100)
	private String street;

	@Column(name = "delivery_postal_code", length = 5)
	private String postalCode;

	@Column(name = "delivery_city", length = 60)
	private String city;

	protected DeliveryAddress() {
		// für JPA
	}

	public DeliveryAddress(String name, String street, String postalCode, String city) {
		this.name = name;
		this.street = street;
		this.postalCode = postalCode;
		this.city = city;
	}

	public String getName() {
		return name;
	}

	public String getStreet() {
		return street;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getCity() {
		return city;
	}

	/** Alte Bestellungen ohne Adresse: JPA legt dann trotzdem ein Objekt an, nur mit leeren Feldern. */
	boolean isEmpty() {
		return name == null && street == null && postalCode == null && city == null;
	}
}
