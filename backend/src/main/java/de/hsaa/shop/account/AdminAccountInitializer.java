package de.hsaa.shop.account;

import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Legt beim ersten Start einen Admin an, falls es noch keinen gibt (ADR-0008).
 * Das Passwort steht nicht im Repo (Q3): Entweder kommt es aus der Umgebungsvariable
 * ADMIN_PASSWORD, oder es wird zufällig erzeugt und einmalig ins Log geschrieben.
 * Auslesen: {@code docker compose logs backend} und nach "Admin-Zugang" suchen (siehe README).
 */
@Component
@Order(1)
public class AdminAccountInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);
	private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

	private final AppUserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final String adminEmail;
	private final String adminPassword;

	public AdminAccountInitializer(AppUserRepository users, PasswordEncoder passwordEncoder,
			@Value("${shop.admin.email:admin@hyrox-shop.local}") String adminEmail,
			@Value("${shop.admin.password:}") String adminPassword) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.adminEmail = AccountService.normalizeEmail(adminEmail);
		this.adminPassword = adminPassword;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (users.existsByRole(Role.ADMIN)) {
			return;
		}
		boolean generated = adminPassword == null || adminPassword.isBlank();
		String password = generated ? randomPassword() : adminPassword;
		users.save(new AppUser(adminEmail, "Shop-Admin", passwordEncoder.encode(password), Role.ADMIN));

		if (generated) {
			log.warn("""

					============================================================
					 Admin-Zugang angelegt (wird nur dieses eine Mal angezeigt)
					   E-Mail:   {}
					   Passwort: {}
					============================================================""", adminEmail, password);
		}
		else {
			log.info("Admin-Zugang {} mit dem Passwort aus ADMIN_PASSWORD angelegt.", adminEmail);
		}
	}

	private static String randomPassword() {
		SecureRandom random = new SecureRandom();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 16; i++) {
			sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}
}
