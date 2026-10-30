package de.hsaa.shop.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import de.hsaa.shop.account.AppUser;
import de.hsaa.shop.account.AppUserRepository;
import de.hsaa.shop.account.Role;
import de.hsaa.shop.product.Product;
import de.hsaa.shop.product.ProductRepository;

/**
 * Bestellen, Bestand, Preise vom Server, Status (docs/anforderungen/0009–0011, B1–B4).
 * Grundlage sind die Beispielprodukte, z. B. "Kreatin Neutral" (24,90 €, 40 Stück) und
 * "Kreatin Zitrone" (4 Stück).
 */
@SpringBootTest
@Transactional
class OrderApiTests {

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
		users.save(new AppUser("markus@example.de", "Markus", passwordEncoder.encode("geheim1234"), Role.CUSTOMER));
	}

	private static RequestPostProcessor lena() {
		return user("lena@example.de").roles("CUSTOMER");
	}

	private static RequestPostProcessor markus() {
		return user("markus@example.de").roles("CUSTOMER");
	}

	private static RequestPostProcessor admin() {
		return user("admin@hyrox-shop.local").roles("ADMIN");
	}

	private Product product(String name) {
		return products.findAll().stream().filter(p -> p.getName().equals(name)).findFirst().orElseThrow();
	}

	private String order(Product product, int quantity) {
		return "{ \"items\": [ { \"productId\": %d, \"quantity\": %d } ] }".formatted(product.getId(), quantity);
	}

	private long placeOrder(RequestPostProcessor who, Product product, int quantity) throws Exception {
		String json = mvc.perform(post("/api/orders").with(who)
					.contentType(MediaType.APPLICATION_JSON).content(order(product, quantity)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		String afterId = json.substring(json.indexOf("\"id\":") + 5);
		return Long.parseLong(afterId.substring(0, afterId.indexOf(',')).trim());
	}

	private void setStatus(RequestPostProcessor who, long orderId, String status, int expectedHttp) throws Exception {
		mvc.perform(patch("/api/orders/" + orderId).with(who)
					.contentType(MediaType.APPLICATION_JSON).content("{ \"status\": \"" + status + "\" }"))
			.andExpect(status().is(expectedHttp));
	}

	// ---- B1, B3: bestellen, Preise vom Server ----

	@Test
	void orderUsesServerPriceAndReducesStock() throws Exception {
		Product kreatin = product("Kreatin Neutral");
		int before = kreatin.getStock();

		// Der Browser schickt einen gefälschten Preis mit, der Server ignoriert ihn.
		mvc.perform(post("/api/orders").with(lena())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"items\": [ { \"productId\": %d, \"quantity\": 2, \"price\": 0.01 } ], \"total\": 0.02 }"
							.formatted(kreatin.getId())))
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.status").value("RECEIVED"))
			.andExpect(jsonPath("$.total").value(49.8))
			.andExpect(jsonPath("$.lines[0].unitPrice").value(24.9))
			.andExpect(jsonPath("$.itemCount").value(2));

		assertEquals(before - 2, product("Kreatin Neutral").getStock());
	}

	@Test
	void sameProductTwiceIsMerged() throws Exception {
		Product kreatin = product("Kreatin Neutral");
		mvc.perform(post("/api/orders").with(lena())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"items\": [ { \"productId\": %d, \"quantity\": 1 }, { \"productId\": %d, \"quantity\": 2 } ] }"
							.formatted(kreatin.getId(), kreatin.getId())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.lines.length()").value(1))
			.andExpect(jsonPath("$.lines[0].quantity").value(3));
	}

	@Test
	void emptyCartIsRejected() throws Exception {
		mvc.perform(post("/api/orders").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content("{ \"items\": [] }"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void invalidQuantityIsRejected() throws Exception {
		mvc.perform(post("/api/orders").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(order(product("Kreatin Neutral"), 0)))
			.andExpect(status().isBadRequest());
	}

	// ---- B2: nichts verkaufen, was nicht auf Lager ist ----

	@Test
	void orderingMoreThanStockIsRejectedAndNothingChanges() throws Exception {
		Product zitrone = product("Kreatin Zitrone"); // 4 Stück
		Product neutral = product("Kreatin Neutral");
		int neutralBefore = neutral.getStock();

		mvc.perform(post("/api/orders").with(lena())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"items\": [ { \"productId\": %d, \"quantity\": 1 }, { \"productId\": %d, \"quantity\": 5 } ] }"
							.formatted(neutral.getId(), zitrone.getId())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.items[0].productName").value("Kreatin Zitrone"))
			.andExpect(jsonPath("$.items[0].available").value(4));

		// Alles oder nichts: auch das verfügbare Produkt wurde nicht abgezogen.
		assertEquals(neutralBefore, product("Kreatin Neutral").getStock());
		assertEquals(4, product("Kreatin Zitrone").getStock());
	}

	@Test
	void soldOutProductCannotBeOrdered() throws Exception {
		mvc.perform(post("/api/orders").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(order(product("Whey Protein Erdbeere"), 1)))
			.andExpect(status().isConflict());
	}

	@Test
	void lastPiecesCanBeBoughtExactly() throws Exception {
		Product zitrone = product("Kreatin Zitrone");
		placeOrder(lena(), zitrone, 4);
		assertEquals(0, product("Kreatin Zitrone").getStock());
		mvc.perform(post("/api/orders").with(markus())
					.contentType(MediaType.APPLICATION_JSON).content(order(zitrone, 1)))
			.andExpect(status().isConflict());
	}

	// ---- A1: eigene Bestellungen wiederfinden ----

	@Test
	void customersOnlySeeTheirOwnOrders() throws Exception {
		long lenasOrder = placeOrder(lena(), product("EAA Mango"), 1);

		mvc.perform(get("/api/orders").with(lena()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalItems").value(1))
			.andExpect(jsonPath("$.items[0].customer.email").value("lena@example.de"));

		mvc.perform(get("/api/orders").with(markus()))
			.andExpect(jsonPath("$.totalItems").value(0));
		mvc.perform(get("/api/orders/" + lenasOrder).with(markus()))
			.andExpect(status().isNotFound());
	}

	// ---- B4: Admin sieht und bearbeitet Bestellungen ----

	@Test
	void adminSeesAllOrdersAndCanFilterByStatus() throws Exception {
		placeOrder(lena(), product("EAA Mango"), 1);
		placeOrder(markus(), product("EAA Cola"), 1);

		mvc.perform(get("/api/orders").with(admin()))
			.andExpect(jsonPath("$.totalItems").value(2))
			.andExpect(jsonPath("$.items[0].allowedNextStatuses.length()").value(2));
		mvc.perform(get("/api/orders").param("status", "SHIPPED").with(admin()))
			.andExpect(jsonPath("$.totalItems").value(0));
	}

	@Test
	void adminMovesOrderThroughAllowedStatuses() throws Exception {
		long id = placeOrder(lena(), product("EAA Mango"), 1);
		setStatus(admin(), id, "PROCESSING", 200);
		setStatus(admin(), id, "SHIPPED", 200);
		setStatus(admin(), id, "CANCELLED", 409); // versendet → nicht mehr stornierbar
		setStatus(admin(), id, "DELIVERED", 200);
		setStatus(admin(), id, "PROCESSING", 409); // zugestellt → Endstatus
	}

	@Test
	void cancellingReturnsTheStock() throws Exception {
		Product mango = product("EAA Mango");
		int before = mango.getStock();
		long id = placeOrder(lena(), mango, 3);
		assertEquals(before - 3, product("EAA Mango").getStock());

		setStatus(lena(), id, "CANCELLED", 200);
		assertEquals(before, product("EAA Mango").getStock());
	}

	@Test
	void customerCannotSetOtherStatusesOrCancelLate() throws Exception {
		long id = placeOrder(lena(), product("EAA Mango"), 1);
		setStatus(lena(), id, "SHIPPED", 403);
		setStatus(admin(), id, "PROCESSING", 200);
		setStatus(lena(), id, "CANCELLED", 409);
	}

	@Test
	void unknownStatusIsRejected() throws Exception {
		long id = placeOrder(lena(), product("EAA Mango"), 1);
		setStatus(admin(), id, "VERLOREN", 400);
	}
}
