package de.hsaa.shop.account;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Eine im Konto gespeicherte Lieferadresse (docs/anforderungen/0021, Sprint 3 E2).
 * Beim Bestellen wird sie in die Bestellung kopiert (ADR-0009): Ändern oder Löschen hier
 * verändert also keine alte Bestellung (E3).
 */
@Entity
@Table(name = "saved_address")
public class SavedAddress {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", nullable = false)
	private AppUser owner;

	/** Optionaler Name wie "Zuhause" oder "Arbeit", damit man die Adressen auseinanderhält. */
	@Column(length = 30)
	private String label;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 100)
	private String street;

	@Column(nullable = false, length = 5)
	private String postalCode;

	@Column(nullable = false, length = 60)
	private String city;

	/** Standardadresse: ist beim Bestellen vorausgewählt. Pro Konto genau eine. */
	@Column(nullable = false)
	private boolean standard;

	@Column(nullable = false)
	private Instant createdAt;

	protected SavedAddress() {
		// für JPA
	}

	public SavedAddress(AppUser owner) {
		this.owner = owner;
		this.createdAt = Instant.now();
	}

	void update(String label, String name, String street, String postalCode, String city) {
		this.label = label;
		this.name = name;
		this.street = street;
		this.postalCode = postalCode;
		this.city = city;
	}

	public Long getId() {
		return id;
	}

	public AppUser getOwner() {
		return owner;
	}

	public String getLabel() {
		return label;
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

	public boolean isStandard() {
		return standard;
	}

	void setStandard(boolean standard) {
		this.standard = standard;
	}
}
