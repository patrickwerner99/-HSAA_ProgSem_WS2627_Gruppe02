package de.hsaa.shop.product;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

/** Sprint 3, G1: Produkte für einen Zeitraum in den Sale stellen, Sale-Bereich, Ersparnis. */
@SpringBootTest
@Transactional
class SaleApiTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private ProductRepository products;

	@Autowired
	private AppUserRepository users;

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

	private Product product(String name, String size) {
		return products.findAll().stream()
				.filter(p -> p.getName().equals(name) && size.equals(p.getSize()))
				.findFirst().orElseThrow();
	}

	private String sale(String price, LocalDate from, LocalDate until) {
		return "{ \"salePrice\": %s, \"from\": \"%s\", \"until\": \"%s\" }".formatted(price, from, until);
	}

	@Test
	void sampleDataHasProductsOnSale() throws Exception {
		mvc.perform(get("/api/products").param("sale", "true").param("size", "50"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalItems").value(4))
			.andExpect(jsonPath("$.items[*].onSale", everyItem(is(true))));
	}

	@Test
	void productOnSaleShowsCurrentAndRegularPriceAndSaving() throws Exception {
		Product vanille = product("Whey Protein Vanille", "1 kg");
		mvc.perform(get("/api/products/" + vanille.getId()))
			.andExpect(jsonPath("$.price").value(27.9))
			.andExpect(jsonPath("$.regularPrice").value(34.9))
			.andExpect(jsonPath("$.onSale").value(true))
			.andExpect(jsonPath("$.savingPercent").value(20));
	}

	@Test
	void adminPutsProductOnSaleAndEndsIt() throws Exception {
		Product kreatin = product("Kreatin Neutral", "500 g");
		LocalDate today = ShopTime.today();
		mvc.perform(put("/api/products/" + kreatin.getId() + "/sale").with(admin())
					.contentType(MediaType.APPLICATION_JSON).content(sale("19.90", today, today.plusDays(7))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.price").value(19.9))
			.andExpect(jsonPath("$.regularPrice").value(24.9))
			.andExpect(jsonPath("$.saleUntil").value(today.plusDays(7).toString()));

		mvc.perform(delete("/api/products/" + kreatin.getId() + "/sale").with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.price").value(24.9))
			.andExpect(jsonPath("$.onSale").value(false));
	}

	@Test
	void futureSaleDoesNotChangeThePriceYet() throws Exception {
		Product kreatin = product("Kreatin Neutral", "500 g");
		LocalDate next = ShopTime.today().plusDays(10);
		mvc.perform(put("/api/products/" + kreatin.getId() + "/sale").with(admin())
					.contentType(MediaType.APPLICATION_JSON).content(sale("19.90", next, next.plusDays(3))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.price").value(24.9))
			.andExpect(jsonPath("$.onSale").value(false));
	}

	@Test
	void salePriceMustBeBelowRegularPrice() throws Exception {
		Product kreatin = product("Kreatin Neutral", "500 g");
		LocalDate today = ShopTime.today();
		mvc.perform(put("/api/products/" + kreatin.getId() + "/sale").with(admin())
					.contentType(MediaType.APPLICATION_JSON).content(sale("29.90", today, today.plusDays(7))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("salePrice"));
	}

	@Test
	void saleEndMustNotBeBeforeStartOrInThePast() throws Exception {
		Product kreatin = product("Kreatin Neutral", "500 g");
		LocalDate today = ShopTime.today();
		mvc.perform(put("/api/products/" + kreatin.getId() + "/sale").with(admin())
					.contentType(MediaType.APPLICATION_JSON).content(sale("19.90", today, today.minusDays(1))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("until"));
		mvc.perform(put("/api/products/" + kreatin.getId() + "/sale").with(admin())
					.contentType(MediaType.APPLICATION_JSON)
					.content(sale("19.90", today.minusDays(10), today.minusDays(2))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("until"));
	}

	@Test
	void onlyAdminsManageSales() throws Exception {
		Product kreatin = product("Kreatin Neutral", "500 g");
		LocalDate today = ShopTime.today();
		mvc.perform(put("/api/products/" + kreatin.getId() + "/sale").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(sale("19.90", today, today.plusDays(7))))
			.andExpect(status().isForbidden());
		mvc.perform(delete("/api/products/" + kreatin.getId() + "/sale"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void sortingByPriceUsesTheSalePrice() throws Exception {
		// Energy-Gel 1,99 € ist sonst das günstigste Produkt; im Sale für 0,99 € bleibt es vorn,
		// ein anderes Produkt für 0,49 € rutscht davor.
		Product riegel = product("Proteinriegel Erdnuss", "45 g");
		LocalDate today = ShopTime.today();
		mvc.perform(put("/api/products/" + riegel.getId() + "/sale").with(admin())
					.contentType(MediaType.APPLICATION_JSON).content(sale("0.49", today, today.plusDays(1))))
			.andExpect(status().isOk());
		mvc.perform(get("/api/products").param("sort", "price-asc"))
			.andExpect(jsonPath("$.items[0].name").value("Proteinriegel Erdnuss"))
			.andExpect(jsonPath("$.items[0].price").value(0.49));
	}

	@Test
	void sortBySavingShowsBiggestDiscountFirst() throws Exception {
		mvc.perform(get("/api/products").param("sale", "true").param("sort", "saving"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].name").value("Whey Protein Vanille"));
	}

	@Test
	void orderUsesTheSalePrice() throws Exception {
		Product vanille = product("Whey Protein Vanille", "1 kg");
		mvc.perform(post("/api/orders").with(lena())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "items": [ { "productId": %d, "quantity": 2 } ],
							  "deliveryAddress": { "name": "Lena Muster", "street": "Beethovenstraße 1",
							    "postalCode": "73430", "city": "Aalen" } }
							""".formatted(vanille.getId())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.lines[0].unitPrice").value(27.9))
			.andExpect(jsonPath("$.total").value(55.8));
		assertEquals(new BigDecimal("34.90"), product("Whey Protein Vanille", "1 kg").getPrice());
	}
}
