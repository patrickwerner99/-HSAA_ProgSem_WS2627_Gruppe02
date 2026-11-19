package de.hsaa.shop.nutrition;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import de.hsaa.shop.product.Product;
import de.hsaa.shop.product.ProductRepository;

/**
 * Sprint 3, H1/H2: Nährwerte aus Open Food Facts. Statt der echten API läuft im Test ein kleiner
 * HTTP-Server, der Antworten, unbekannte Barcodes, Fehler und eine zu langsame API nachstellt.
 */
@SpringBootTest
@Transactional
class NutritionApiTests {

	private static final String SPORTNESS = "4066447398083";
	private static final String JSON = """
			{ "code": "4066447398083", "status": 1, "status_verbose": "product found",
			  "product": { "product_name": "WHEY Protein Schoko-Geschmack", "brands": "Sportness",
			    "nutriscore_grade": "unknown",
			    "nutriments": { "energy_100g": 1582, "proteins_100g": 78, "carbohydrates_100g": 5.4,
			      "sugars_100g": 3.2, "fat_100g": 4.9, "saturated-fat_100g": 2.1, "salt_100g": 0.6 } } }
			""";

	/** Bekannt, aber ohne Nährwerte (kommt bei Open Food Facts vor). */
	private static final String EMPTY = "4058172511097";
	private static final String EMPTY_JSON = """
			{ "status": 1, "product": { "product_name": "WHEY Protein Schoko-Geschmack", "brands": "Sportness",
			  "nutriments": {} } }
			""";

	/** Was der Fake-Server gerade tun soll. */
	enum Mode { OK, DOWN, SLOW }

	private static volatile Mode mode = Mode.OK;
	private static final AtomicInteger calls = new AtomicInteger();
	private static final HttpServer server = startServer();

	@DynamicPropertySource
	static void openFoodFacts(DynamicPropertyRegistry registry) {
		registry.add("shop.openfoodfacts.base-url", () -> "http://localhost:" + server.getAddress().getPort());
		registry.add("shop.openfoodfacts.timeout-ms", () -> "400");
	}

	private static HttpServer startServer() {
		try {
			HttpServer s = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			s.createContext("/api/v2/product/", NutritionApiTests::handle);
			// Mehrere Threads: Eine absichtlich langsame Antwort blockiert so nicht den nächsten Test.
			s.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
			s.start();
			return s;
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static void handle(HttpExchange exchange) throws IOException {
		calls.incrementAndGet();
		if (mode == Mode.SLOW) {
			try {
				Thread.sleep(1500);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
		if (exchange.getRequestURI().getPath().contains(EMPTY)) {
			byte[] empty = EMPTY_JSON.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, empty.length);
			exchange.getResponseBody().write(empty);
			exchange.close();
			return;
		}
		boolean known = exchange.getRequestURI().getPath().contains(SPORTNESS);
		int code = mode == Mode.DOWN ? 500 : known ? 200 : 404;
		String body = mode == Mode.DOWN ? "kaputt" : known ? JSON : "{ \"status\": 0, \"status_verbose\": \"product not found\" }";
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(code, bytes.length);
		exchange.getResponseBody().write(bytes);
		exchange.close();
	}

	@AfterAll
	static void stopServer() {
		server.stop(0);
	}

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private ProductRepository products;

	@Autowired
	private NutritionService nutritionService;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		mode = Mode.OK;
		calls.set(0);
		nutritionService.clearCache();
	}

	private Product withEan() {
		return products.findAll().stream().filter(p -> SPORTNESS.equals(p.getEan())).findFirst().orElseThrow();
	}

	private Product withoutEan() {
		return products.findAll().stream().filter(p -> p.getEan() == null).findFirst().orElseThrow();
	}

	@Test
	void showsNutritionFromOpenFoodFacts() throws Exception {
		mvc.perform(get("/api/products/" + withEan().getId() + "/nutrition"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.source").value("Open Food Facts"))
			.andExpect(jsonPath("$.productName").value("WHEY Protein Schoko-Geschmack"))
			.andExpect(jsonPath("$.brands").value("Sportness"))
			.andExpect(jsonPath("$.protein").value(78))
			.andExpect(jsonPath("$.energyKcal").value(378))
			.andExpect(jsonPath("$.stale").value(false));
	}

	@Test
	void secondCallComesFromTheCache() throws Exception {
		long id = withEan().getId();
		mvc.perform(get("/api/products/" + id + "/nutrition")).andExpect(status().isOk());
		mvc.perform(get("/api/products/" + id + "/nutrition")).andExpect(status().isOk());
		org.junit.jupiter.api.Assertions.assertEquals(1, calls.get());
	}

	@Test
	void productWithoutBarcodeIs404() throws Exception {
		mvc.perform(get("/api/products/" + withoutEan().getId() + "/nutrition"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Für dieses Produkt sind keine Nährwerte hinterlegt."));
	}

	@Test
	void unknownBarcodeIs404() throws Exception {
		Product product = withoutEan();
		product.setEan("4000000000000");
		mvc.perform(get("/api/products/" + product.getId() + "/nutrition"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Open Food Facts kennt dieses Produkt nicht."));
	}

	@Test
	void productWithoutNutritionValuesIs404() throws Exception {
		Product product = withoutEan();
		product.setEan(EMPTY);
		mvc.perform(get("/api/products/" + product.getId() + "/nutrition"))
			.andExpect(status().isNotFound());
	}

	@Test
	void apiDownWithoutCacheIs503ButTheProductStillLoads() throws Exception {
		mode = Mode.DOWN;
		long id = withEan().getId();
		mvc.perform(get("/api/products/" + id + "/nutrition"))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.title").value("Gerade nicht verfügbar"));
		mvc.perform(get("/api/products/" + id)).andExpect(status().isOk());
	}

	@Test
	void slowApiTimesOutQuickly() throws Exception {
		mode = Mode.SLOW;
		long start = System.currentTimeMillis();
		mvc.perform(get("/api/products/" + withEan().getId() + "/nutrition"))
			.andExpect(status().isServiceUnavailable());
		org.junit.jupiter.api.Assertions.assertTrue(System.currentTimeMillis() - start < 1400,
				"Der Shop hat zu lange auf die externe API gewartet.");
	}

	@Test
	void apiDownWithCacheReturnsStaleValues() throws Exception {
		long id = withEan().getId();
		mvc.perform(get("/api/products/" + id + "/nutrition")).andExpect(status().isOk());
		nutritionService.expireCacheForTests();
		mode = Mode.DOWN;
		mvc.perform(get("/api/products/" + id + "/nutrition"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.protein").value(78))
			.andExpect(jsonPath("$.stale").value(true));
	}

	@Test
	void invalidBarcodeIsAFieldError() throws Exception {
		long id = withoutEan().getId();
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/products/" + id)
					.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
							.user("admin@hyrox-shop.local").roles("ADMIN"))
					.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
					.content("{ \"ean\": \"12ab\" }"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("ean"));
	}
}
