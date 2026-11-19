package de.hsaa.shop.returns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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

/** Sprint 3, F1/F2: Ware zurückschicken, Stand sehen, Admin bearbeitet, Rückläufer als reduzierte Ware. */
@SpringBootTest
@Transactional
class ReturnApiTests {

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

	private static RequestPostProcessor admin() {
		return user("admin@hyrox-shop.local").roles("ADMIN");
	}

	private Product kreatin() {
		return products.findAll().stream()
				.filter(p -> p.getName().equals("Kreatin Neutral") && "500 g".equals(p.getSize()) && !p.isReturnedGoods())
				.findFirst().orElseThrow();
	}

	private static long idOf(String json) {
		String afterId = json.substring(json.indexOf("\"id\":") + 5);
		return Long.parseLong(afterId.substring(0, afterId.indexOf(',')).trim());
	}

	/** Bestellung über 3 × Kreatin anlegen und auf Wunsch bis "Zugestellt" bringen. */
	private long order(String discountCode, boolean deliver) throws Exception {
		String body = mvc.perform(post("/api/orders").with(lena())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "items": [ { "productId": %d, "quantity": 3 } ], %s
							  "deliveryAddress": { "name": "Lena Muster", "street": "Beethovenstraße 1",
							    "postalCode": "73430", "city": "Aalen" } }
							""".formatted(kreatin().getId(),
							discountCode == null ? "" : "\"discountCode\": \"" + discountCode + "\",")))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		long id = idOf(body);
		if (deliver) {
			for (String s : new String[] { "PROCESSING", "SHIPPED", "DELIVERED" }) {
				mvc.perform(patch("/api/orders/" + id).with(admin())
							.contentType(MediaType.APPLICATION_JSON).content("{ \"status\": \"" + s + "\" }"))
					.andExpect(status().isOk());
			}
		}
		return id;
	}

	private String returnJson(long orderId, int quantity) {
		return """
				{ "orderId": %d, "reason": "Falsche Sorte bestellt",
				  "items": [ { "productId": %d, "quantity": %d } ] }
				""".formatted(orderId, kreatin().getId(), quantity);
	}

	private long requestReturn(long orderId, int quantity) throws Exception {
		return idOf(mvc.perform(post("/api/returns").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(returnJson(orderId, quantity)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString());
	}

	private void setStatus(long returnId, String json, int expected) throws Exception {
		mvc.perform(patch("/api/returns/" + returnId).with(admin())
					.contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().is(expected));
	}

	private String received(String condition) {
		return """
				{ "status": "RECEIVED", "conditions": [ { "productId": %d, "condition": "%s" } ] }
				""".formatted(kreatin().getId(), condition);
	}

	// ---- F1 ----

	@Test
	void customerReturnsPartOfADeliveredOrder() throws Exception {
		long orderId = order(null, true);
		mvc.perform(post("/api/returns").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(returnJson(orderId, 2)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("REQUESTED"))
			.andExpect(jsonPath("$.lines[0].quantity").value(2))
			.andExpect(jsonPath("$.expectedRefund").value(49.8));
		mvc.perform(get("/api/orders/" + orderId + "/returns").with(lena()))
			.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void onlyDeliveredOrdersCanBeReturned() throws Exception {
		long orderId = order(null, false);
		mvc.perform(post("/api/returns").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(returnJson(orderId, 1)))
			.andExpect(status().isConflict());
	}

	@Test
	void cannotReturnMoreThanWasOrdered() throws Exception {
		long orderId = order(null, true);
		requestReturn(orderId, 2);
		mvc.perform(post("/api/returns").with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(returnJson(orderId, 2)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Von „Kreatin Neutral (500 g)“ kannst du höchstens 1 Stück zurückschicken."));
	}

	@Test
	void foreignOrdersAndReturnsAreInvisible() throws Exception {
		long orderId = order(null, true);
		long returnId = requestReturn(orderId, 1);
		mvc.perform(post("/api/returns").with(markus())
					.contentType(MediaType.APPLICATION_JSON).content(returnJson(orderId, 1)))
			.andExpect(status().isNotFound());
		mvc.perform(get("/api/returns/" + returnId).with(markus())).andExpect(status().isNotFound());
		mvc.perform(get("/api/returns").with(markus())).andExpect(jsonPath("$.totalItems").value(0));
	}

	@Test
	void missingReasonIsRejected() throws Exception {
		long orderId = order(null, true);
		mvc.perform(post("/api/returns").with(lena())
					.contentType(MediaType.APPLICATION_JSON)
					.content(returnJson(orderId, 1).replace("Falsche Sorte bestellt", "")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("reason"));
	}

	@Test
	void customersCannotProcessReturns() throws Exception {
		long returnId = requestReturn(order(null, true), 1);
		mvc.perform(patch("/api/returns/" + returnId).with(lena())
					.contentType(MediaType.APPLICATION_JSON).content(received("UNUSED")))
			.andExpect(status().isForbidden());
	}

	// ---- F2 ----

	@Test
	void unusedItemsComeBackAsReducedGoods() throws Exception {
		long returnId = requestReturn(order(null, true), 2);
		setStatus(returnId, received("UNUSED"), 200);

		Product twin = products.findFirstByReturnedGoodsTrueAndNameAndSize("Kreatin Neutral", "500 g").orElseThrow();
		assertEquals(2, twin.getStock());
		assertEquals(new BigDecimal("24.90"), twin.getPrice());
		assertEquals(new BigDecimal("17.43"), twin.getSalePrice());
		mvc.perform(get("/api/products/" + twin.getId()))
			.andExpect(jsonPath("$.returnedGoods").value(true))
			.andExpect(jsonPath("$.onSale").value(true))
			.andExpect(jsonPath("$.price").value(17.43));
		mvc.perform(get("/api/products").param("sale", "true").param("size", "50"))
			.andExpect(jsonPath("$.totalItems").value(5));
	}

	@Test
	void secondReturnAddsToTheSameReducedProduct() throws Exception {
		setStatus(requestReturn(order(null, true), 1), received("UNUSED"), 200);
		setStatus(requestReturn(order(null, true), 2), received("UNUSED"), 200);
		assertEquals(3, products.findFirstByReturnedGoodsTrueAndNameAndSize("Kreatin Neutral", "500 g")
				.orElseThrow().getStock());
	}

	@Test
	void usedItemsAreNotSoldAgain() throws Exception {
		long returnId = requestReturn(order(null, true), 1);
		setStatus(returnId, received("USED"), 200);
		assertTrue(products.findFirstByReturnedGoodsTrueAndNameAndSize("Kreatin Neutral", "500 g").isEmpty());
	}

	@Test
	void receivingNeedsAConditionForEveryItem() throws Exception {
		long returnId = requestReturn(order(null, true), 1);
		setStatus(returnId, "{ \"status\": \"RECEIVED\" }", 400);
	}

	@Test
	void refundUsesOrderPricesAndDiscount() throws Exception {
		long returnId = requestReturn(order("HYROX10", true), 2);
		setStatus(returnId, received("USED"), 200);
		mvc.perform(patch("/api/returns/" + returnId).with(admin())
					.contentType(MediaType.APPLICATION_JSON).content("{ \"status\": \"REFUNDED\" }"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REFUNDED"))
			.andExpect(jsonPath("$.refundAmount").value(44.82));
		// Kunde sieht den Stand jederzeit (F1)
		mvc.perform(get("/api/returns/" + returnId).with(lena()))
			.andExpect(jsonPath("$.status").value("REFUNDED"));
	}

	@Test
	void statusesFollowTheAllowedOrder() throws Exception {
		long returnId = requestReturn(order(null, true), 1);
		setStatus(returnId, "{ \"status\": \"REFUNDED\" }", 409);
		setStatus(returnId, "{ \"status\": \"REJECTED\", \"note\": \"Ware kam nie an\" }", 200);
		setStatus(returnId, received("UNUSED"), 409);
		mvc.perform(get("/api/returns/" + returnId).with(lena()))
			.andExpect(jsonPath("$.note").value("Ware kam nie an"));
	}

	@Test
	void rejectedReturnsDoNotCountAgainstTheQuantity() throws Exception {
		long orderId = order(null, true);
		setStatus(requestReturn(orderId, 3), "{ \"status\": \"REJECTED\" }", 200);
		requestReturn(orderId, 3);
	}
}
