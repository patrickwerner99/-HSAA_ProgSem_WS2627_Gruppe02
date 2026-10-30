package de.hsaa.shop.security;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Wer darf was (docs/anforderungen/0007, ADR-0004)?
 *
 * <ul>
 * <li>Lesen von Produkten, Kategorien und Bildern: alle, auch ohne Login</li>
 * <li>Registrieren, Anmelden, Abmelden: alle</li>
 * <li>Produkte, Kategorien und Bilder ändern: nur ADMIN, sonst 401 (nicht angemeldet) bzw. 403 (kein Admin)</li>
 * <li>Bestellungen: nur angemeldet; welche man sehen und ändern darf, prüft der OrderService</li>
 * </ul>
 *
 * Die Regeln gelten für jeden Aufruf der API, egal ob er aus dem Frontend oder z. B. aus der
 * .http-Datei kommt (Anforderung A2).
 */
@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contextRepository)
			throws Exception {
		http
			// CSRF-Schutz übernimmt das SameSite=Strict-Cookie (application.properties), siehe ADR-0004.
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.securityContext(context -> context.securityContextRepository(contextRepository))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/error", "/api/health").permitAll()
				.requestMatchers("/api/auth/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/images/**").permitAll()
				.requestMatchers("/api/products", "/api/products/**").hasRole("ADMIN")
				.requestMatchers("/api/categories", "/api/categories/**").hasRole("ADMIN")
				.requestMatchers("/api/orders", "/api/orders/**").authenticated()
				.anyRequest().authenticated())
			.exceptionHandling(errors -> errors
				.authenticationEntryPoint((request, response, ex) -> writeProblem(response, 401,
						"Nicht angemeldet", "Bitte melde dich an."))
				.accessDeniedHandler((request, response, ex) -> writeProblem(response, 403,
						"Keine Berechtigung", "Das darf nur ein Admin.")));
		return http.build();
	}

	/** Speichert die Anmeldung in der HTTP-Sitzung (Cookie JSESSIONID). */
	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	/** Passwörter werden mit BCrypt gehasht, nie im Klartext gespeichert. */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/** Gleiches Format wie alle anderen Fehler (Problem Details, ADR-0003). */
	private static void writeProblem(HttpServletResponse response, int status, String title, String detail)
			throws IOException {
		response.setStatus(status);
		response.setContentType("application/problem+json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status
				+ ",\"detail\":\"" + detail + "\"}");
	}
}
