package de.hsaa.shop.discount;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Ein Rabattcode wie "HYROX10" (Sprint 3, G2, docs/anforderungen/0024): so viel Prozent auf die ganze
 * Bestellung, im Zeitraum von {@code validFrom} bis einschließlich {@code validUntil}, optional begrenzt
 * auf eine Anzahl Einlösungen.
 */
@Entity
@Table(name = "discount_code")
public class DiscountCode {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Immer in Großbuchstaben gespeichert, damit "hyrox10" und "HYROX10" derselbe Code sind. */
	@Column(nullable = false, unique = true, length = 20)
	private String code;

	@Column(nullable = false)
	private int percent;

	@Column(nullable = false)
	private LocalDate validFrom;

	@Column(nullable = false)
	private LocalDate validUntil;

	/** null = beliebig oft einlösbar. */
	private Integer maxUses;

	@Column(nullable = false)
	private int timesUsed;

	/** Der Admin kann einen Code sofort abschalten, ohne ihn zu löschen. */
	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false)
	private Instant createdAt;

	protected DiscountCode() {
		// für JPA
	}

	public DiscountCode(String code, int percent, LocalDate validFrom, LocalDate validUntil, Integer maxUses) {
		this.code = code;
		this.percent = percent;
		this.validFrom = validFrom;
		this.validUntil = validUntil;
		this.maxUses = maxUses;
		this.createdAt = Instant.now();
	}

	/** Warum der Code heute nicht einlösbar ist, oder null, wenn er gilt. */
	public String problem(LocalDate today) {
		if (!active) {
			return "Dieser Rabattcode ist nicht mehr gültig.";
		}
		if (today.isBefore(validFrom)) {
			return "Dieser Rabattcode gilt erst ab " + validFrom + ".";
		}
		if (today.isAfter(validUntil)) {
			return "Dieser Rabattcode ist abgelaufen.";
		}
		if (maxUses != null && timesUsed >= maxUses) {
			return "Dieser Rabattcode wurde schon zu oft eingelöst.";
		}
		return null;
	}

	void use() {
		timesUsed++;
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public int getPercent() {
		return percent;
	}

	public LocalDate getValidFrom() {
		return validFrom;
	}

	public LocalDate getValidUntil() {
		return validUntil;
	}

	public Integer getMaxUses() {
		return maxUses;
	}

	public int getTimesUsed() {
		return timesUsed;
	}

	public boolean isActive() {
		return active;
	}

	void setActive(boolean active) {
		this.active = active;
	}

	void setValidUntil(LocalDate validUntil) {
		this.validUntil = validUntil;
	}
}
