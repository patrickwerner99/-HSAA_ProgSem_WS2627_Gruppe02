package de.hsaa.shop.product;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import de.hsaa.shop.category.CategoryRepository;

/**
 * Prüft die Produkt-API gegen die Akzeptanzkriterien aus docs/anforderungen/0001–0005.
 * Grundlage sind die 13 Beispielprodukte, die beim Start eingespielt werden.
 * {@code @Transactional}: Jede Änderung wird nach dem Test zurückgerollt.
 */
@SpringBootTest
@Transactional
class ProductApiTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private CategoryRepository categories;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	private long categoryId(String name) {
		return categories.findByNameIgnoreCase(name).orElseThrow().getId();
	}

	private String validProduct(long categoryId) {
		return """
				{ "name": "Kreatin Mango", "description": "Test", "price": 24.9, "stock": 10,
				  "imageUrl": "/images/kreatin-neutral.svg", "categoryId": %d }
				""".formatted(categoryId);
	}

	// ---- 0001 Produktliste ----

	@Test
	void firstLoadReturnsTwelveOfThirteenSampleProducts() throws Exception {
		mvc.perform(get("/api/products"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", hasSize(12)))
			.andExpect(jsonPath("$.totalItems").value(13))
			.andExpect(jsonPath("$.last").value(false));
	}

	@Test
	void secondLoadReturnsTheRest() throws Exception {
		mvc.perform(get("/api/products").param("page", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.last").value(true));
	}

	@Test
	void searchIgnoresCase() throws Exception {
		mvc.perform(get("/api/products").param("q", "WHEY"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalItems").value(4));
	}

	@Test
	void searchAndCategoryCombine() throws Exception {
		mvc.perform(get("/api/products")
					.param("q", "schoko")
					.param("categoryId", String.valueOf(categoryId("Protein"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalItems").value(1))
			.andExpect(jsonPath("$.items[0].name").value("Whey Protein Schoko"));
	}

	@Test
	void searchWithoutHitsReturnsEmptyList() throws Exception {
		mvc.perform(get("/api/products").param("q", "gibtsnicht"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", hasSize(0)))
			.andExpect(jsonPath("$.totalItems").value(0));
	}

	// ---- 0002 Details und Bestand ----

	@Test
	void sampleDataContainsEveryStockStatus() throws Exception {
		mvc.perform(get("/api/products").param("size", "50"))
			.andExpect(jsonPath("$.items[*].stockStatus", hasItem("IN_STOCK")))
			.andExpect(jsonPath("$.items[*].stockStatus", hasItem("LOW_STOCK")))
			.andExpect(jsonPath("$.items[*].stockStatus", hasItem("SOLD_OUT")));
	}

	@Test
	void unknownProductReturns404WithMessage() throws Exception {
		mvc.perform(get("/api/products/999999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Dieses Produkt gibt es nicht (mehr)."));
	}

	@Test
	void nonNumericIdReturns400() throws Exception {
		mvc.perform(get("/api/products/abc"))
			.andExpect(status().isBadRequest());
	}

	// ---- 0003 Pflege ----

	@Test
	void createReturns201AndLocation() throws Exception {
		mvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validProduct(categoryId("Kreatin"))))
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.name").value("Kreatin Mango"))
			.andExpect(jsonPath("$.stockStatus").value("IN_STOCK"))
			.andExpect(jsonPath("$.categoryName").value("Kreatin"));
	}

	@Test
	void putReplacesWholeProduct() throws Exception {
		long id = createProduct();
		mvc.perform(put("/api/products/" + id)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "name": "Kreatin Mango XL", "price": 29.9, "stock": 3,
							  "categoryId": %d }
							""".formatted(categoryId("Protein"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Kreatin Mango XL"))
			.andExpect(jsonPath("$.description").doesNotExist())
			.andExpect(jsonPath("$.stockStatus").value("LOW_STOCK"))
			.andExpect(jsonPath("$.categoryName").value("Protein"));
	}

	@Test
	void patchChangesOnlyTheGivenField() throws Exception {
		long id = createProduct();
		mvc.perform(patch("/api/products/" + id)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"stock\": 0 }"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.stock").value(0))
			.andExpect(jsonPath("$.stockStatus").value("SOLD_OUT"))
			.andExpect(jsonPath("$.name").value("Kreatin Mango"))
			.andExpect(jsonPath("$.price").value(24.9))
			.andExpect(jsonPath("$.description").value("Test"));
	}

	@Test
	void deleteRemovesProduct() throws Exception {
		long id = createProduct();
		mvc.perform(delete("/api/products/" + id)).andExpect(status().isNoContent());
		mvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
	}

	// ---- 0005 Fehlermeldungen ----

	@Test
	void invalidProductReturns400WithFieldErrors() throws Exception {
		mvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "name": "", "price": 0, "stock": -1, "imageUrl": "kein-link",
							  "categoryId": null }
							"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Ungültige Eingabe"))
			.andExpect(jsonPath("$.errors[*].field", hasItem("name")))
			.andExpect(jsonPath("$.errors[*].field", hasItem("price")))
			.andExpect(jsonPath("$.errors[*].field", hasItem("stock")))
			.andExpect(jsonPath("$.errors[*].field", hasItem("imageUrl")))
			.andExpect(jsonPath("$.errors[*].field", hasItem("categoryId")))
			.andExpect(jsonPath("$.errors[*].message", hasItem("Der Preis muss größer als 0 sein.")));
	}

	@Test
	void fractionalStockIsRejected() throws Exception {
		mvc.perform(patch("/api/products/" + createProduct())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"stock\": 3.5 }"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].message").value("Der Bestand muss eine ganze Zahl ab 0 sein."));
	}

	@Test
	void unknownCategoryIsAFieldError() throws Exception {
		mvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validProduct(999999)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("categoryId"));
	}

	@Test
	void brokenJsonReturns400WithoutTechnicalDetails() throws Exception {
		mvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"price\": \"abc\" "))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail", not(org.hamcrest.Matchers.containsString("Exception"))));
	}

	private long createProduct() throws Exception {
		String json = mvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validProduct(categoryId("Kreatin"))))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		// {"id":17,...} → 17, ohne extra JSON-Bibliothek
		String afterId = json.substring(json.indexOf("\"id\":") + 5);
		return Long.parseLong(afterId.substring(0, afterId.indexOf(',')).trim());
	}
}
