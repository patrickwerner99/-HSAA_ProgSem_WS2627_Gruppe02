package de.hsaa.shop.image;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import de.hsaa.shop.product.ProductRepository;

/** Produktbilder hochladen und ausliefern (docs/anforderungen/0014, D1–D3). */
@SpringBootTest
@Transactional
class ImageApiTests {

	/** Kleinstes gültiges PNG (1×1 Pixel). */
	private static final byte[] PNG = java.util.Base64.getDecoder().decode(
			"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private ProductRepository products;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	private static RequestPostProcessor admin() {
		return user("admin@hyrox-shop.local").roles("ADMIN");
	}

	private long anyProductId() {
		return products.findAll().get(0).getId();
	}

	@Test
	void adminUploadsImageAndApiReturnsUrl() throws Exception {
		long id = anyProductId();
		String json = mvc.perform(multipart("/api/products/" + id + "/image")
					.file(new MockMultipartFile("file", "dose.png", "image/png", PNG)).with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imageUrl", startsWith("/api/images/")))
			.andReturn().getResponse().getContentAsString();

		String url = json.replaceAll(".*\"imageUrl\":\"([^\"]+)\".*", "$1");
		mvc.perform(get(url))
			.andExpect(status().isOk())
			.andExpect(content().contentType("image/png"))
			.andExpect(content().bytes(PNG));
	}

	@Test
	void otherFileTypesAreRejected() throws Exception {
		mvc.perform(multipart("/api/products/" + anyProductId() + "/image")
					.file(new MockMultipartFile("file", "notiz.txt", "text/plain", "hallo".getBytes())).with(admin()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("file"));
	}

	@Test
	void customersCannotUploadImages() throws Exception {
		mvc.perform(multipart("/api/products/" + anyProductId() + "/image")
					.file(new MockMultipartFile("file", "dose.png", "image/png", PNG))
					.with(user("kunde@example.de").roles("CUSTOMER")))
			.andExpect(status().isForbidden());
	}

	@Test
	void removingImageLeavesProductWithoutImage() throws Exception {
		mvc.perform(delete("/api/products/" + anyProductId() + "/image").with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imageUrl").doesNotExist());
	}

	@Test
	void unknownOrInvalidImageNamesReturn404() throws Exception {
		mvc.perform(get("/api/images/00000000-0000-0000-0000-000000000000.png")).andExpect(status().isNotFound());
		// Nur selbst vergebene Namen (UUID + Bildendung) werden überhaupt gesucht.
		mvc.perform(get("/api/images/application.properties")).andExpect(status().isNotFound());
	}
}
