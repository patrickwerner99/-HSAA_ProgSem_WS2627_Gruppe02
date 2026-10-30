package de.hsaa.shop.account;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Lädt ein Konto für Spring Security anhand der E-Mail-Adresse.
 * Weil es diese Klasse gibt, legt Spring Boot keinen eigenen Test-Benutzer mit Zufallspasswort an.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

	private final AppUserRepository users;

	public AppUserDetailsService(AppUserRepository users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		AppUser user = users.findByEmail(AccountService.normalizeEmail(email))
				.orElseThrow(() -> new UsernameNotFoundException("Kein Konto"));
		return User.withUsername(user.getEmail())
				.password(user.getPasswordHash())
				.authorities(user.getRole().authority())
				.build();
	}
}
