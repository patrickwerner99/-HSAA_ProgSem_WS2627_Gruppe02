package de.hsaa.shop.account;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.hsaa.shop.account.AccountDtos.AccountUpdateRequest;
import de.hsaa.shop.account.AccountDtos.AddressRequest;
import de.hsaa.shop.account.AccountDtos.AddressResponse;
import de.hsaa.shop.account.AccountDtos.PasswordChangeRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * Das eigene Konto unter /api/account (nur angemeldet, Sprint 3 E1/E2).
 *
 * <pre>
 * GET    /api/account                    eigene Kontodaten                     200 / 401
 * PATCH  /api/account                    Name / E-Mail ändern                  200 / 400 / 401 / 409
 * PUT    /api/account/password           Passwort ändern                       204 / 400 / 401
 * GET    /api/account/addresses          gespeicherte Adressen                 200 / 401
 * POST   /api/account/addresses          Adresse anlegen                       201 / 400 / 401 / 409
 * PUT    /api/account/addresses/{id}     Adresse ändern (auch: Standard)       200 / 400 / 401 / 404
 * DELETE /api/account/addresses/{id}     Adresse löschen                       204 / 401 / 404
 * </pre>
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

	private final AccountSettingsService service;
	private final SecurityContextRepository securityContextRepository;

	public AccountController(AccountSettingsService service, SecurityContextRepository securityContextRepository) {
		this.service = service;
		this.securityContextRepository = securityContextRepository;
	}

	@GetMapping
	public UserResponse get() {
		return service.get();
	}

	@PatchMapping
	public UserResponse update(@Valid @RequestBody AccountUpdateRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		AppUser user = service.update(request);
		updateSessionLogin(user, httpRequest, httpResponse);
		return UserResponse.from(user);
	}

	@PutMapping("/password")
	public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
		service.changePassword(request);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/addresses")
	public List<AddressResponse> addresses() {
		return service.addresses();
	}

	@PostMapping("/addresses")
	public ResponseEntity<AddressResponse> addAddress(@Valid @RequestBody AddressRequest request) {
		AddressResponse created = service.addAddress(request);
		return ResponseEntity.created(URI.create("/api/account/addresses/" + created.id())).body(created);
	}

	@PutMapping("/addresses/{id}")
	public AddressResponse updateAddress(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
		return service.updateAddress(id, request);
	}

	@DeleteMapping("/addresses/{id}")
	public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
		service.deleteAddress(id);
		return ResponseEntity.noContent().build();
	}

	/**
	 * In der Sitzung steht die E-Mail als Anmeldename (ADR-0004). Nach einer Änderung muss sie dort
	 * auch neu stehen, sonst fände {@link CurrentUser} das Konto bei der nächsten Anfrage nicht mehr.
	 */
	private void updateSessionLogin(AppUser user, HttpServletRequest request, HttpServletResponse response) {
		Authentication old = SecurityContextHolder.getContext().getAuthentication();
		if (old == null || user.getEmail().equals(old.getName())) {
			return;
		}
		Authentication auth = UsernamePasswordAuthenticationToken.authenticated(user.getEmail(), null,
				old.getAuthorities());
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
	}
}
