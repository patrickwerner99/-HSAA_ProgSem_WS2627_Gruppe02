package de.hsaa.shop.account;

/** Wer gerade angemeldet ist. Enthält nie das Passwort oder den Hash. */
public record UserResponse(Long id, String email, String name, Role role) {

	public static UserResponse from(AppUser user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole());
	}
}
