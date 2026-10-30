package de.hsaa.shop.account;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Registrieren, Anmelden, Abmelden (docs/anforderungen/0006). */
@SpringBootTest
@Transactional
class AuthApiTests {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	private static final String NEW_CUSTOMER = """
			{ "name": "Lena Läuferin", "email": "Lena@Example.de", "password": "geheim1234" }
			""";

	@Test
	void registerCreatesCustomerAndLogsIn() throws Exception {
		MvcResult result = mvc.perform(post("/api/auth/register")
					.contentType(MediaType.APPLICATION_JSON).content(NEW_CUSTOMER))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value("lena@example.de"))
			.andExpect(jsonPath("$.role").value("CUSTOMER"))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist())
			.andReturn();

		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		mvc.perform(get("/api/auth/me").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Lena Läuferin"));
	}

	@Test
	void duplicateEmailIsRejected() throws Exception {
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(NEW_CUSTOMER))
			.andExpect(status().isCreated());
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
					.content(NEW_CUSTOMER.replace("Lena@Example.de", "lena@example.de")))
			.andExpect(status().isConflict());
	}

	@Test
	void invalidRegistrationReturnsFieldErrors() throws Exception {
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
					.content("{ \"name\": \"\", \"email\": \"keine-mail\", \"password\": \"kurz\" }"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
	}

	@Test
	void adminFromStartupCanLogIn() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
					.content("{ \"email\": \"admin@hyrox-shop.local\", \"password\": \"test-admin-passwort\" }"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	void wrongPasswordReturns401WithoutRevealingWhy() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
					.content("{ \"email\": \"admin@hyrox-shop.local\", \"password\": \"falsch\" }"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("E-Mail oder Passwort ist falsch."));
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
					.content("{ \"email\": \"gibtsnicht@example.de\", \"password\": \"egal12345\" }"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("E-Mail oder Passwort ist falsch."));
	}

	@Test
	void meWithoutLoginReturns401() throws Exception {
		mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void logoutEndsSession() throws Exception {
		MvcResult result = mvc.perform(post("/api/auth/register")
					.contentType(MediaType.APPLICATION_JSON).content(NEW_CUSTOMER))
			.andReturn();
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

		mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isNoContent());
		org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid(), "Sitzung muss nach dem Abmelden ungültig sein");
	}
}
