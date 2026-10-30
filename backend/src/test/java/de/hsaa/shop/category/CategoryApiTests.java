package de.hsaa.shop.category;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Prüft die Kategorie-API gegen docs/anforderungen/0003 und 0004. */
@SpringBootTest
@Transactional
class CategoryApiTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private CategoryRepository categories;

	private MockMvc mvc;

	/** Schreibende Aufrufe brauchen seit Sprint 2 einen Admin (ADR-0004). */
	private static RequestPostProcessor admin() {
		return user("admin@hyrox-shop.local").roles("ADMIN");
	}

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	@Test
	void sampleDataHasFourCategoriesWithProductCount() throws Exception {
		mvc.perform(get("/api/categories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(4)))
			.andExpect(jsonPath("$[?(@.name == 'Protein')].productCount").value(4));
	}

	@Test
	void createCategory() throws Exception {
		mvc.perform(post("/api/categories").with(admin())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"name\": \"Recovery\" }"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Recovery"))
			.andExpect(jsonPath("$.productCount").value(0));
	}

	@Test
	void duplicateNameIgnoringCaseIsRejected() throws Exception {
		mvc.perform(post("/api/categories").with(admin())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"name\": \"protein\" }"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Diese Kategorie gibt es schon."));
	}

	@Test
	void emptyNameIsRejected() throws Exception {
		mvc.perform(post("/api/categories").with(admin())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"name\": \"  \" }"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("name"));
	}

	@Test
	void renameCategory() throws Exception {
		long id = categories.findByNameIgnoreCase("Aminosäuren").orElseThrow().getId();
		mvc.perform(put("/api/categories/" + id).with(admin())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"name\": \"Aminos\" }"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Aminos"));
	}

	@Test
	void categoryWithProductsCannotBeDeleted() throws Exception {
		long id = categories.findByNameIgnoreCase("Protein").orElseThrow().getId();
		mvc.perform(delete("/api/categories/" + id).with(admin()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(
					"Die Kategorie enthält noch 4 Produkte. Bitte die Produkte zuerst verschieben oder löschen."));
	}

	@Test
	void emptyCategoryCanBeDeleted() throws Exception {
		long id = categories.save(new Category("Leer")).getId();
		mvc.perform(delete("/api/categories/" + id).with(admin())).andExpect(status().isNoContent());
		mvc.perform(get("/api/categories/" + id)).andExpect(status().isNotFound());
	}
}
