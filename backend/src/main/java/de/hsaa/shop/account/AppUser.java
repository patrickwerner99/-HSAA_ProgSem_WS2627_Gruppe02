package de.hsaa.shop.account;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Ein Konto im Shop. Das Passwort wird nur als BCrypt-Hash gespeichert, nie im Klartext. */
@Entity
@Table(name = "app_user")
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Immer klein geschrieben gespeichert, damit "Max@Mail.de" und "max@mail.de" dasselbe Konto sind. */
	@Column(nullable = false, unique = true, length = 254)
	private String email;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(nullable = false)
	private Instant createdAt;

	protected AppUser() {
		// für JPA
	}

	public AppUser(String email, String name, String passwordHash, Role role) {
		this.email = email;
		this.name = name;
		this.passwordHash = passwordHash;
		this.role = role;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getName() {
		return name;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
