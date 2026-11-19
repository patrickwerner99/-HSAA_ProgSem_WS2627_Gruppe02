package de.hsaa.shop.account;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.hsaa.shop.account.AccountDtos.AccountUpdateRequest;
import de.hsaa.shop.account.AccountDtos.AddressRequest;
import de.hsaa.shop.account.AccountDtos.AddressResponse;
import de.hsaa.shop.account.AccountDtos.PasswordChangeRequest;
import de.hsaa.shop.common.ConflictException;
import de.hsaa.shop.common.InvalidFieldException;
import de.hsaa.shop.common.NotFoundException;

/**
 * Das eigene Konto pflegen (Sprint 3, E1 und E2): Name, E-Mail, Passwort und gespeicherte Adressen.
 * Jede Methode arbeitet nur mit dem angemeldeten Nutzer, fremde Konten und Adressen sind nie erreichbar.
 */
@Service
@Transactional
public class AccountSettingsService {

	/** Mehr Adressen braucht niemand; schützt außerdem vor vollgemüllten Konten. */
	static final int MAX_ADDRESSES = 10;

	private final CurrentUser currentUser;
	private final AppUserRepository users;
	private final SavedAddressRepository addresses;
	private final PasswordEncoder passwordEncoder;

	public AccountSettingsService(CurrentUser currentUser, AppUserRepository users, SavedAddressRepository addresses,
			PasswordEncoder passwordEncoder) {
		this.currentUser = currentUser;
		this.users = users;
		this.addresses = addresses;
		this.passwordEncoder = passwordEncoder;
	}

	// ---- E1: Kontodaten ----

	@Transactional(readOnly = true)
	public UserResponse get() {
		return UserResponse.from(currentUser.require());
	}

	/** Name und/oder E-Mail ändern. Für eine neue E-Mail braucht es das aktuelle Passwort. */
	public AppUser update(AccountUpdateRequest request) {
		AppUser user = currentUser.require();
		if (request.email() != null) {
			String email = AccountService.normalizeEmail(request.email());
			if (!email.equals(user.getEmail())) {
				requirePassword(user, request.currentPassword());
				if (users.existsByEmail(email)) {
					throw new ConflictException("Zu dieser E-Mail-Adresse gibt es schon ein Konto.");
				}
				user.setEmail(email);
			}
		}
		if (request.name() != null) {
			user.setName(request.name().trim());
		}
		return user;
	}

	public void changePassword(PasswordChangeRequest request) {
		AppUser user = currentUser.require();
		requirePassword(user, request.currentPassword());
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
	}

	/** Falsches Passwort ist ein Feldfehler (400), kein 401: angemeldet ist man ja. */
	private void requirePassword(AppUser user, String password) {
		if (password == null || password.isBlank()) {
			throw new InvalidFieldException("currentPassword", "Bitte gib zur Bestätigung dein aktuelles Passwort ein.");
		}
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new InvalidFieldException("currentPassword", "Das aktuelle Passwort stimmt nicht.");
		}
	}

	// ---- E2: Adressen ----

	@Transactional(readOnly = true)
	public List<AddressResponse> addresses() {
		AppUser user = currentUser.require();
		return addresses.findByOwnerIdOrderByStandardDescCreatedAtAsc(user.getId()).stream()
				.map(AddressResponse::from)
				.toList();
	}

	/** Neue Adresse. Die erste Adresse eines Kontos wird automatisch Standard. */
	public AddressResponse addAddress(AddressRequest request) {
		AppUser user = currentUser.require();
		long count = addresses.countByOwnerId(user.getId());
		if (count >= MAX_ADDRESSES) {
			throw new ConflictException("Du kannst höchstens " + MAX_ADDRESSES
					+ " Adressen speichern. Bitte lösche zuerst eine.");
		}
		SavedAddress address = new SavedAddress(user);
		apply(address, request);
		address = addresses.save(address);
		if (count == 0 || Boolean.TRUE.equals(request.standard())) {
			makeStandard(address);
		}
		return AddressResponse.from(address);
	}

	public AddressResponse updateAddress(Long id, AddressRequest request) {
		SavedAddress address = load(id);
		apply(address, request);
		if (Boolean.TRUE.equals(request.standard())) {
			makeStandard(address);
		}
		return AddressResponse.from(address);
	}

	/** Löschen. War es die Standardadresse, wird die älteste übrige Adresse neuer Standard. */
	public void deleteAddress(Long id) {
		SavedAddress address = load(id);
		boolean wasStandard = address.isStandard();
		Long ownerId = address.getOwner().getId();
		addresses.delete(address);
		addresses.flush();
		if (wasStandard) {
			addresses.findByOwnerIdOrderByStandardDescCreatedAtAsc(ownerId).stream()
					.findFirst()
					.ifPresent(next -> next.setStandard(true));
		}
	}

	private SavedAddress load(Long id) {
		AppUser user = currentUser.require();
		return addresses.findByIdAndOwnerId(id, user.getId())
				.orElseThrow(() -> new NotFoundException("Diese Adresse gibt es nicht."));
	}

	private void makeStandard(SavedAddress address) {
		for (SavedAddress other : addresses.findByOwnerIdOrderByStandardDescCreatedAtAsc(address.getOwner().getId())) {
			other.setStandard(other.getId().equals(address.getId()));
		}
	}

	private static void apply(SavedAddress address, AddressRequest request) {
		String label = request.label() == null || request.label().isBlank() ? null : request.label().trim();
		address.update(label, request.name().trim(), request.street().trim(), request.postalCode().trim(),
				request.city().trim());
	}
}
