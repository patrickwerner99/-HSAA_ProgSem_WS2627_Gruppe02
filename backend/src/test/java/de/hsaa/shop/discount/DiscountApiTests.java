package de.hsaa.shop.discount;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

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

import de.hsaa.shop.account.AppUser;
import de.hsaa.shop.account.AppUserRepository;
import de.hsaa.shop.account.Role;
import de.hsaa.shop.common.ShopTime;
import de.hsaa.shop.product.Product;
import de.hsaa.shop.product.ProductRepository;

/** Sprint 3, G2: Admins legen Rabattcodes an, Kunden lösen sie beim Bestellen ein. */
@SpringBootTest
@Transactional
class DiscountApiTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private AppUserRepository users;

	@Autowired
	private ProductRepository products;

	@Autowired
	private DiscountCodeRepository codes;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		users.save(new AppUser("lena@example.de", "Lena", passwordEncoder.encode("geheim1234"), Role.CUSTOMER));
	}

	private static RequestPostProcessor admin() {
		return user("admin@hyrox-shop.local").roles("ADMIN");
	}

	private static RequestPostProcessor lena() {
		return user("lena@example.de").roles("CUSTOMER");
	}

	private Product kreatin() {
		return products.findAll().stream()
				.filter(p -> p.getName().equals("Kreatin Neutral") && "500 g".equals(p.getSize()))
				.findFirst().orElseThrow();
	}

	private String order(String code) {
		return """
				{ "items": [ { "productId": %d, "quantity": 2 } ], "discountCode": "%s",
				  "deliveryAddress": { "name": "Lena Muster", "street": "Beethovenstraße 1",
				    "postalCode": "73430", "city": "Aalen" } }
				""".formatted(kreatin().getId(), code);
	}

	@Test
	void adminCreatesAndListsCodes() throws Exception {
		LocalDate today = ShopTime.today();
		mvc.perform(post("/api/discount-codes").with(admin())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"code\": \"black-friday\", \"percent\": 25, \"validFrom\": \"%s\", \"validUntil\": \"%s\" }"
							.formatted(today, today.plusDays(3))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value("BLACK-FRIDAY"))
			.andExpect(jsonPath("$.usableToday").value(true));
		mvc.perform(get("/api/discount-codes").with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(4)));
	}

	@Test
	void duplicateCodeIsAConflict() throws Exception {
		LocalDate today = ShopTime.today();
		mvc.perform(post("/api/discount-codes").with(admin())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"code\": \"hyrox10\", \"percent\": 5, \"validFrom\": \"%s\", \"validUntil\": \"%s\" }"
							.formatted(today, today)))
			.andExpect(status().isConflict());
	}

	@Test
	void invalidCodeIsRejected() throws Exception {
		LocalDate today = ShopTime.today();
		mvc.perform(post("/api/discount-codes").with(admin())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"code\": \"x\", \"percent\": 80, \"validFrom\": \"%s\", \"validUntil\": \"%s\" }"
							.formatted(today, today.minusDays(1))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.length()").value(2));
	}

	@Test
	void customersCannotManageCodes() throws Exception {
		mvc.perform(get("/api/discount-codes").with(lena())).andExpect(status().isForbidden());
		mvc.perform(post("/api/discount-codes").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/discount-codes/HYROX10")).andExpect(status().isUnauthorized());
	}

	@Test
	void customerChecksACode() throws Exception {
		mvc.perform(get("/api/discount-codes/hyrox10").with(lena()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.percent").value(10));
		mvc.perform(get("/api/discount-codes/SOMMER20").with(lena()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Dieser Rabattcode ist abgelaufen."));
		mvc.perform(get("/api/discount-codes/GIBTSNICHT").with(lena()))
			.andExpect(status().isNotFound());
	}

	@Test
	void orderWithCodeGetsTheDiscount() throws Exception {
		mvc.perform(post("/api/orders").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(order("hyrox10")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.subtotal").value(49.8))
			.andExpect(jsonPath("$.discountCode").value("HYROX10"))
			.andExpect(jsonPath("$.discountAmount").value(4.98))
			.andExpect(jsonPath("$.total").value(44.82));
		assertEquals(1, codes.findByCode("HYROX10").orElseThrow().getTimesUsed());
	}

	@Test
	void invalidCodeStopsTheOrderAndKeepsTheStock() throws Exception {
		int before = kreatin().getStock();
		mvc.perform(post("/api/orders").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(order("SOMMER20")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("discountCode"))
			.andExpect(jsonPath("$.errors[0].message").value("Dieser Rabattcode ist abgelaufen."));
		assertEquals(before, kreatin().getStock());
	}

	@Test
	void deactivatedCodeCannotBeUsed() throws Exception {
		long id = codes.findByCode("HYROX10").orElseThrow().getId();
		mvc.perform(patch("/api/discount-codes/" + id).with(admin())
					.contentType(MediaType.APPLICATION_JSON).content("{ \"active\": false }"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.usableToday").value(false));
		mvc.perform(post("/api/orders").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(order("HYROX10")))
			.andExpect(status().isBadRequest());
	}

	@Test
	void codeWithUsageLimitStopsAtTheLimit() throws Exception {
		LocalDate today = ShopTime.today();
		codes.save(new DiscountCode("EINMAL", 10, today, today, 1));
		mvc.perform(post("/api/orders").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(order("EINMAL")))
			.andExpect(status().isCreated());
		mvc.perform(post("/api/orders").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(order("EINMAL")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].message").value("Dieser Rabattcode wurde schon zu oft eingelöst."));
	}

	@Test
	void usedCodeCanOnlyBeDeactivated() throws Exception {
		mvc.perform(post("/api/orders").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(order("HYROX10")))
			.andExpect(status().isCreated());
		long id = codes.findByCode("HYROX10").orElseThrow().getId();
		mvc.perform(delete("/api/discount-codes/" + id).with(admin())).andExpect(status().isConflict());
		long unused = codes.findByCode("SOMMER20").orElseThrow().getId();
		mvc.perform(delete("/api/discount-codes/" + unused).with(admin())).andExpect(status().isNoContent());
	}
}
