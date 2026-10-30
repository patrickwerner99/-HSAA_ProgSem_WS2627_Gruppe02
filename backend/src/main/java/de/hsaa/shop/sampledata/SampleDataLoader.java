package de.hsaa.shop.sampledata;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import de.hsaa.shop.category.Category;
import de.hsaa.shop.category.CategoryRepository;
import de.hsaa.shop.product.Product;
import de.hsaa.shop.product.ProductRepository;

/**
 * Spielt beim Start das Hyrox-Sortiment ein, aber nur, wenn noch keine Kategorie existiert
 * (docs/anforderungen/0004). So überschreibt ein Neustart keine Änderungen aus der Pflegeseite.
 * Zurücksetzen: {@code docker-compose down -v}, dann ist die Datenbank wieder leer.
 */
@Component
public class SampleDataLoader implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SampleDataLoader.class);

	private final CategoryRepository categories;
	private final ProductRepository products;

	public SampleDataLoader(CategoryRepository categories, ProductRepository products) {
		this.categories = categories;
		this.products = products;
	}

	@Override
	public void run(ApplicationArguments args) {
		try {
			if (categories.count() > 0) {
				log.info("Datenbank enthält schon Daten, Beispieldaten werden nicht eingespielt.");
				return;
			}
			List<Product> sample = createSampleProducts();
			products.saveAll(sample);
			log.info("Beispieldaten eingespielt: {} Kategorien, {} Produkte.", categories.count(), sample.size());
		}
		catch (RuntimeException ex) {
			// Ein Fehler hier darf den Shop nicht am Starten hindern (Fehlerfall in 0004).
			log.error("Beispieldaten konnten nicht eingespielt werden.", ex);
		}
	}

	private List<Product> createSampleProducts() {
		Category kreatin = categories.save(new Category("Kreatin"));
		Category protein = categories.save(new Category("Protein"));
		Category amino = categories.save(new Category("Aminosäuren"));
		Category hyrox = categories.save(new Category("Hyrox-Supplement"));

		String kreatinText = "Kreatin-Monohydrat, 500 g Pulver. Für mehr Kraft und Schnellkraft an Sled Push, "
				+ "Sandbag Lunges und Wall Balls. 5 g täglich in Wasser oder Saft einrühren.";
		String wheyText = "Whey-Protein, 1 kg Pulver, rund 24 g Eiweiß pro Portion. "
				+ "Für Muskelaufbau und schnelle Regeneration nach harten Einheiten.";
		String eaaText = "Essenzielle Aminosäuren (EAA), 500 g Pulver. Zum Mischen ins Wasser, "
				+ "ideal während langer Läufe und Doppel-Einheiten.";
		String hyroxText = "Unser eigenes Supplement, speziell für Hyrox entwickelt. 500 g Pulver. "
				+ "Die Rezeptur bleibt unser Geheimnis.";

		List<Product> list = new ArrayList<>();
		list.add(product("Kreatin Neutral", kreatinText, "24.90", 40, "kreatin-neutral.svg", kreatin));
		list.add(product("Kreatin Zitrone", kreatinText, "24.90", 4, "kreatin-zitrone.svg", kreatin));
		list.add(product("Kreatin Kirsche", kreatinText, "24.90", 18, "kreatin-kirsche.svg", kreatin));

		list.add(product("Whey Protein Schoko", wheyText, "34.90", 60, "whey-schoko.svg", protein));
		list.add(product("Whey Protein Vanille", wheyText, "34.90", 35, "whey-vanille.svg", protein));
		list.add(product("Whey Protein Erdbeere", wheyText, "34.90", 0, "whey-erdbeere.svg", protein));
		list.add(product("Whey Protein Cookies", wheyText, "36.90", 12, "whey-cookies.svg", protein));

		list.add(product("EAA Mango", eaaText, "27.90", 22, "eaa-mango.svg", amino));
		list.add(product("EAA Eistee Pfirsich", eaaText, "27.90", 3, "eaa-eistee-pfirsich.svg", amino));
		list.add(product("EAA Cola", eaaText, "27.90", 15, "eaa-cola.svg", amino));

		list.add(product("Hyrox-Supplement Orange", hyroxText, "39.90", 25, "hyrox-orange.svg", hyrox));
		list.add(product("Hyrox-Supplement Beere", hyroxText, "39.90", 9, "hyrox-beere.svg", hyrox));
		list.add(product("Hyrox-Supplement Zitrone-Limette", hyroxText, "39.90", 2, "hyrox-zitrone-limette.svg", hyrox));
		return list;
	}

	private static Product product(String name, String description, String price, int stock, String image,
			Category category) {
		return new Product(name, description, new BigDecimal(price), stock, "/images/" + image, category);
	}
}
