package de.hsaa.shop.account;

import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import de.hsaa.shop.common.UnauthorizedException;

/** Wer stellt gerade die Anfrage? Liest die Anmeldung aus der Sitzung (Spring Security). */
@Component
public class CurrentUser {

	private final AppUserRepository users;

	public CurrentUser(AppUserRepository users) {
		this.users = users;
	}

	public Optional<AppUser> get() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
			return Optional.empty();
		}
		return users.findByEmail(AccountService.normalizeEmail(auth.getName()));
	}

	/** Angemeldeter Nutzer oder 401. */
	public AppUser require() {
		return get().orElseThrow(() -> new UnauthorizedException("Bitte melde dich an."));
	}

	public boolean isAdmin() {
		return get().map(user -> user.getRole() == Role.ADMIN).orElse(false);
	}
}
