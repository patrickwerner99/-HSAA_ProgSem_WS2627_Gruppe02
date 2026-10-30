package de.hsaa.shop.account;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.hsaa.shop.common.ConflictException;
import de.hsaa.shop.common.UnauthorizedException;

/** Registrieren und Anmelden (docs/anforderungen/0006). */
@Service
@Transactional
public class AccountService {

	/** Absichtlich gleiche Meldung für "E-Mail unbekannt" und "Passwort falsch": verrät nicht, welche Konten es gibt. */
	static final String LOGIN_FAILED = "E-Mail oder Passwort ist falsch.";

	private final AppUserRepository users;
	private final AppUserDetailsService userDetails;
	private final PasswordEncoder passwordEncoder;

	public AccountService(AppUserRepository users, AppUserDetailsService userDetails,
			PasswordEncoder passwordEncoder) {
		this.users = users;
		this.userDetails = userDetails;
		this.passwordEncoder = passwordEncoder;
	}

	/** Legt ein Kundenkonto an. Admins entstehen nie über die API, nur beim Start (AdminAccountInitializer). */
	public AppUser register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (users.existsByEmail(email)) {
			throw new ConflictException("Zu dieser E-Mail-Adresse gibt es schon ein Konto. Bitte melde dich an.");
		}
		AppUser user = new AppUser(email, request.name().trim(), passwordEncoder.encode(request.password()),
				Role.CUSTOMER);
		return users.save(user);
	}

	/** Prüft E-Mail und Passwort und liefert die Daten für die Sitzung. */
	@Transactional(readOnly = true)
	public UserDetails verify(LoginRequest request) {
		try {
			UserDetails details = userDetails.loadUserByUsername(request.email());
			if (passwordEncoder.matches(request.password(), details.getPassword())) {
				return details;
			}
		}
		catch (UsernameNotFoundException ex) {
			// gleiche Antwort wie bei falschem Passwort
		}
		throw new UnauthorizedException(LOGIN_FAILED);
	}

	@Transactional(readOnly = true)
	public AppUser findByEmail(String email) {
		return users.findByEmail(normalizeEmail(email))
				.orElseThrow(() -> new UnauthorizedException("Bitte melde dich an."));
	}

	static String normalizeEmail(String email) {
		return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
	}
}
