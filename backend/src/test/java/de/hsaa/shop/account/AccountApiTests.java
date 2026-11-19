package de.hsaa.shop.account;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import de.hsaa.shop.product.Product;
import de.hsaa.shop.product.ProductRepository;

/** Sprint 3 E1/E2: Kontodaten selbst pflegen, mehrere Adressen speichern und beim Bestellen wählen. */
@SpringBootTest
@Transactional
class AccountApiTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private AppUserRepository users;

	@Autowired
	private ProductRepository products;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		users.save(new AppUser("lena@example.de", "Lena", passwordEncoder.encode("geheim1234"), Role.CUSTOMER));
		users.save(new AppUser("markus@example.de", "Markus", passwordEncoder.encode("geheim1234"), Role.CUSTOMER));
	}

	private static RequestPostProcessor lena() {
		return user("lena@example.de").roles("CUSTOMER");
	}

	private static RequestPostProcessor markus() {
		return user("markus@example.de").roles("CUSTOMER");
	}

	private static String address(String label, String street) {
		return """
				{ "label": "%s", "name": "Lena Muster", "street": "%s", "postalCode": "73430", "city": "Aalen" }
				""".formatted(label, street);
	}

	private long addAddress(RequestPostProcessor who, String json) throws Exception {
		String body = mvc.perform(post("/api/account/addresses").with(who)
					.contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		String afterId = body.substring(body.indexOf("\"id\":") + 5);
		return Long.parseLong(afterId.substring(0, afterId.indexOf(',')).trim());
	}

	// ---- E1: Kontodaten ----

	@Test
	void accountRequiresLogin() throws Exception {
		mvc.perform(get("/api/account")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/account/addresses")).andExpect(status().isUnauthorized());
	}

	@Test
	void customerChangesName() throws Exception {
		mvc.perform(patch("/api/account").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content("{ \"name\": \"Lena Muster\" }"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Lena Muster"))
			.andExpect(jsonPath("$.email").value("lena@example.de"));
	}

	@Test
	void changingEmailNeedsTheCurrentPassword() throws Exception {
		mvc.perform(patch("/api/account").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content("{ \"email\": \"neu@example.de\" }"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("currentPassword"));

		mvc.perform(patch("/api/account").with(lena())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"email\": \"Neu@Example.de\", \"currentPassword\": \"geheim1234\" }"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("neu@example.de"));
		assertTrue(users.findByEmail("neu@example.de").isPresent());
	}

	@Test
	void emailOfAnotherAccountIsAConflict() throws Exception {
		mvc.perform(patch("/api/account").with(lena())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"email\": \"markus@example.de\", \"currentPassword\": \"geheim1234\" }"))
			.andExpect(status().isConflict());
	}

	@Test
	void passwordChangeChecksTheOldPassword() throws Exception {
		mvc.perform(put("/api/account/password").with(lena())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"currentPassword\": \"falsch\", \"newPassword\": \"neuesPasswort1\" }"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("currentPassword"));

		mvc.perform(put("/api/account/password").with(lena())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"currentPassword\": \"geheim1234\", \"newPassword\": \"neuesPasswort1\" }"))
			.andExpect(status().isNoContent());
		assertTrue(passwordEncoder.matches("neuesPasswort1",
				users.findByEmail("lena@example.de").orElseThrow().getPasswordHash()));
	}

	@Test
	void tooShortNewPasswordIsRejected() throws Exception {
		mvc.perform(put("/api/account/password").with(lena())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"currentPassword\": \"geheim1234\", \"newPassword\": \"kurz\" }"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("newPassword"));
	}

	// ---- E2: Adressen ----

	@Test
	void firstAddressBecomesStandard() throws Exception {
		mvc.perform(post("/api/account/addresses").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(address("Zuhause", "Beethovenstraße 1")))
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.standard").value(true));
		mvc.perform(post("/api/account/addresses").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(address("Arbeit", "Anton-Huber-Straße 25")))
			.andExpect(jsonPath("$.standard").value(false));

		mvc.perform(get("/api/account/addresses").with(lena()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].label").value("Zuhause"));
	}

	@Test
	void anotherAddressCanBecomeStandard() throws Exception {
		addAddress(lena(), address("Zuhause", "Beethovenstraße 1"));
		long work = addAddress(lena(), address("Arbeit", "Anton-Huber-Straße 25"));
		mvc.perform(put("/api/account/addresses/" + work).with(lena())
					.contentType(MediaType.APPLICATION_JSON)
					.content(address("Arbeit", "Anton-Huber-Straße 25").replace("}", ", \"standard\": true }")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.standard").value(true));
		mvc.perform(get("/api/account/addresses").with(lena()))
			.andExpect(jsonPath("$[0].label").value("Arbeit"))
			.andExpect(jsonPath("$[1].standard").value(false));
	}

	@Test
	void deletingTheStandardAddressMakesAnotherOneStandard() throws Exception {
		long home = addAddress(lena(), address("Zuhause", "Beethovenstraße 1"));
		addAddress(lena(), address("Arbeit", "Anton-Huber-Straße 25"));
		mvc.perform(delete("/api/account/addresses/" + home).with(lena())).andExpect(status().isNoContent());
		mvc.perform(get("/api/account/addresses").with(lena()))
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].standard").value(true));
	}

	@Test
	void invalidAddressIsRejected() throws Exception {
		mvc.perform(post("/api/account/addresses").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(address("Zuhause", "Beethovenstraße")
							.replace("73430", "123")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field", hasItem("street")))
			.andExpect(jsonPath("$.errors[*].field", hasItem("postalCode")));
	}

	@Test
	void foreignAddressesAreInvisible() throws Exception {
		long lenas = addAddress(lena(), address("Zuhause", "Beethovenstraße 1"));
		mvc.perform(get("/api/account/addresses").with(markus())).andExpect(jsonPath("$", hasSize(0)));
		mvc.perform(delete("/api/account/addresses/" + lenas).with(markus())).andExpect(status().isNotFound());
	}

	@Test
	void atMostTenAddresses() throws Exception {
		for (int i = 1; i <= AccountSettingsService.MAX_ADDRESSES; i++) {
			addAddress(lena(), address("Adresse " + i, "Beethovenstraße " + i));
		}
		mvc.perform(post("/api/account/addresses").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(address("Zu viel", "Beethovenstraße 99")))
			.andExpect(status().isConflict());
	}

	// ---- E2 + E3: Adresse beim Bestellen wählen, Bestellung bleibt unverändert ----

	@Test
	void orderWithSavedAddressKeepsACopy() throws Exception {
		long home = addAddress(lena(), address("Zuhause", "Beethovenstraße 1"));
		Product product = products.findAll().getFirst();

		String body = mvc.perform(post("/api/orders").with(lena())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"items\": [ { \"productId\": %d, \"quantity\": 1 } ], \"addressId\": %d }"
							.formatted(product.getId(), home)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.deliveryAddress.street").value("Beethovenstraße 1"))
			.andReturn().getResponse().getContentAsString();
		String afterId = body.substring(body.indexOf("\"id\":") + 5);
		long orderId = Long.parseLong(afterId.substring(0, afterId.indexOf(',')).trim());

		// Adresse im Konto ändern und löschen: die Bestellung zeigt weiter die alte (E3, ADR-0009).
		mvc.perform(put("/api/account/addresses/" + home).with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(address("Zuhause", "Bahnhofstraße 7")))
			.andExpect(status().isOk());
		mvc.perform(delete("/api/account/addresses/" + home).with(lena())).andExpect(status().isNoContent());
		mvc.perform(get("/api/orders/" + orderId).with(lena()))
			.andExpect(jsonPath("$.deliveryAddress.street").value("Beethovenstraße 1"));
	}

	@Test
	void foreignAddressCannotBeUsedForAnOrder() throws Exception {
		long lenas = addAddress(lena(), address("Zuhause", "Beethovenstraße 1"));
		Product product = products.findAll().getFirst();
		int before = product.getStock();
		mvc.perform(post("/api/orders").with(markus())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"items\": [ { \"productId\": %d, \"quantity\": 1 } ], \"addressId\": %d }"
							.formatted(product.getId(), lenas)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("addressId"));
		assertEquals(before, products.findById(product.getId()).orElseThrow().getStock());
	}
}
