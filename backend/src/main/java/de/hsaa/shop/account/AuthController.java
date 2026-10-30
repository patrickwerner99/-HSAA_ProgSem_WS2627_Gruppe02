package de.hsaa.shop.account;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * Anmeldung per Sitzung (Session-Cookie), siehe ADR-0004.
 *
 * <pre>
 * POST /api/auth/register   Konto anlegen und direkt angemeldet sein   201 / 400 / 409
 * POST /api/auth/login      anmelden                                   200 / 400 / 401
 * POST /api/auth/logout     abmelden                                   204
 * GET  /api/auth/me         wer bin ich?                               200 / 401
 * </pre>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AccountService accounts;
	private final CurrentUser currentUser;
	private final SecurityContextRepository securityContextRepository;

	public AuthController(AccountService accounts, CurrentUser currentUser,
			SecurityContextRepository securityContextRepository) {
		this.accounts = accounts;
		this.currentUser = currentUser;
		this.securityContextRepository = securityContextRepository;
	}

	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		AppUser user = accounts.register(request);
		startSession(user.getEmail(), List.of(user.getRole().authority()), httpRequest, httpResponse);
		return ResponseEntity.created(URI.create("/api/auth/me")).body(UserResponse.from(user));
	}

	@PostMapping("/login")
	public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		UserDetails details = accounts.verify(request);
		List<String> roles = details.getAuthorities().stream().map(a -> a.getAuthority()).toList();
		startSession(details.getUsername(), roles, httpRequest, httpResponse);
		return UserResponse.from(accounts.findByEmail(details.getUsername()));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
		HttpSession session = httpRequest.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public UserResponse me() {
		return UserResponse.from(currentUser.require());
	}

	/**
	 * Speichert die Anmeldung in einer neuen Sitzung. Die alte Sitzung wird verworfen,
	 * damit niemand eine vorher bekannte Sitzungs-ID weiterverwenden kann (Session Fixation).
	 */
	private void startSession(String email, List<String> roles, HttpServletRequest request,
			HttpServletResponse response) {
		HttpSession old = request.getSession(false);
		if (old != null) {
			old.invalidate();
		}
		request.getSession(true);

		Authentication auth = UsernamePasswordAuthenticationToken.authenticated(email, null,
				roles.stream().map(SimpleGrantedAuthority::new).toList());
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
	}
}
