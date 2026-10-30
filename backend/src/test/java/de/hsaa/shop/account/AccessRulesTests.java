package de.hsaa.shop.account;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

/**
 * A2: Was nur Admins dürfen, ist für alle anderen gesperrt, auch beim direkten Aufruf der API
 * (docs/anforderungen/0007).
 */
@SpringBootTest
@Transactional
class AccessRulesTests {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	private static final String CATEGORY = "{ \"name\": \"Recovery\" }";

	@Test
	void everybodyCanReadProductsAndCategories() throws Exception {
		mvc.perform(get("/api/products")).andExpect(status().isOk());
		mvc.perform(get("/api/categories")).andExpect(status().isOk());
	}

	@Test
	void anonymousCannotChangeAnythingAndGets401AsProblemDetails() throws Exception {
		mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(CATEGORY))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
			.andExpect(jsonPath("$.status").value(401));
		mvc.perform(delete("/api/products/1")).andExpect(status().isUnauthorized());
	}

	@Test
	void customerCannotUseAdminApiAndGets403() throws Exception {
		mvc.perform(post("/api/categories").with(user("kunde@example.de").roles("CUSTOMER"))
					.contentType(MediaType.APPLICATION_JSON).content(CATEGORY))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.detail").value("Das darf nur ein Admin."));
		mvc.perform(delete("/api/products/1").with(user("kunde@example.de").roles("CUSTOMER")))
			.andExpect(status().isForbidden());
	}

	@Test
	void adminCanUseAdminApi() throws Exception {
		mvc.perform(post("/api/categories").with(user("admin@hyrox-shop.local").roles("ADMIN"))
					.contentType(MediaType.APPLICATION_JSON).content(CATEGORY))
			.andExpect(status().isCreated());
	}

	@Test
	void ordersNeedLogin() throws Exception {
		mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
					.content("{ \"items\": [ { \"productId\": 1, \"quantity\": 1 } ] }"))
			.andExpect(status().isUnauthorized());
	}
}
