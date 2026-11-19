package de.hsaa.shop.sampledata;

import static de.hsaa.shop.product.ProductForm.GEL;
import static de.hsaa.shop.product.ProductForm.GETRAENK;
import static de.hsaa.shop.product.ProductForm.KAPSELN;
import static de.hsaa.shop.product.ProductForm.PULVER;
import static de.hsaa.shop.product.ProductForm.RIEGEL;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import de.hsaa.shop.category.Category;
import de.hsaa.shop.category.CategoryRepository;
import de.hsaa.shop.common.ShopTime;
import de.hsaa.shop.discount.DiscountCode;
import de.hsaa.shop.discount.DiscountCodeRepository;
import de.hsaa.shop.product.Product;
import de.hsaa.shop.product.ProductForm;
import de.hsaa.shop.product.ProductRepository;

/**
 * Spielt beim Start das Hyrox-Sortiment ein, aber nur, wenn noch keine Kategorie existiert
 * (docs/anforderungen/0004, erweitert in 0020: 31 Produkte in 5 Kategorien, verschiedene Größen und
 * Produktarten). So überschreibt ein Neustart keine Änderungen aus der Pflegeseite.
 * Zurücksetzen: {@code docker-compose down -v}, dann ist die Datenbank wieder leer.
 */
@Component
public class SampleDataLoader implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SampleDataLoader.class);

	private final CategoryRepository categories;
	private final ProductRepository products;
	private final DiscountCodeRepository discountCodes;

	public SampleDataLoader(CategoryRepository categories, ProductRepository products,
			DiscountCodeRepository discountCodes) {
		this.categories = categories;
		this.products = products;
		this.discountCodes = discountCodes;
	}

	@Override
	public void run(ApplicationArguments args) {
		try {
			if (categories.count() > 0) {
				log.info("Datenbank enthält schon Daten, Beispieldaten werden nicht eingespielt.");
				return;
			}
			List<Product> sample = createSampleProducts();
			startSampleSales(sample);
			addSampleBarcodes(sample);
			products.saveAll(sample);
			createSampleDiscountCodes();
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
		Category energie = categories.save(new Category("Energie & Elektrolyte"));

		String kreatinText = "Kreatin-Monohydrat als Pulver. Für mehr Kraft und Schnellkraft an Sled Push, "
				+ "Sandbag Lunges und Wall Balls. 5 g täglich in Wasser oder Saft einrühren.";
		String kreatinKapselnText = "Kreatin-Monohydrat in Kapseln, 4 Kapseln entsprechen 5 g. "
				+ "Praktisch für unterwegs und ohne Geschmack.";
		String wheyText = "Whey-Protein als Pulver, rund 24 g Eiweiß pro Portion (30 g). "
				+ "Für Muskelaufbau und schnelle Regeneration nach harten Einheiten.";
		String riegelText = "Proteinriegel mit 20 g Eiweiß und wenig Zucker. "
				+ "Der Snack für die Sporttasche, direkt nach dem Training oder zwischendurch.";
		String shakeText = "Fertiger Proteinshake mit 25 g Eiweiß, gekühlt am besten. Kein Anrühren, kein Shaker.";
		String eaaText = "Essenzielle Aminosäuren (EAA) als Pulver. Zum Mischen ins Wasser, "
				+ "ideal während langer Läufe und Doppel-Einheiten.";
		String eaaDrinkText = "Essenzielle Aminosäuren als fertiges Getränk, ohne Kohlensäure.";
		String hyroxText = "Unser eigenes Supplement, speziell für Hyrox entwickelt. "
				+ "Die Rezeptur bleibt unser Geheimnis.";
		String elektrolytText = "Isotonisches Getränk mit Natrium, Kalium und Magnesium. "
				+ "Gegen Krämpfe in der zweiten Rennhälfte.";
		String tabsText = "Elektrolyt-Brausetabletten, eine Tablette auf 500 ml Wasser.";
		String gelText = "Energy-Gel mit 25 g schnellen Kohlenhydraten. Ideal ab Station 5, mit Wasser einnehmen.";
		String energieRiegelText = "Haferriegel mit Banane, 40 g Kohlenhydrate. Für den Energieschub vor dem Start.";

		List<Product> list = new ArrayList<>();
		// Kreatin: Pulver in drei Größen, Sorten und als Kapseln
		list.add(product("Kreatin Neutral", kreatinText, "24.90", 40, "kreatin-neutral.svg", kreatin, "500 g", PULVER));
		list.add(product("Kreatin Zitrone", kreatinText, "24.90", 4, "kreatin-zitrone.svg", kreatin, "500 g", PULVER));
		list.add(product("Kreatin Kirsche", kreatinText, "24.90", 18, "kreatin-kirsche.svg", kreatin, "500 g", PULVER));
		list.add(product("Kreatin Neutral", kreatinText, "14.90", 30, "kreatin-neutral.svg", kreatin, "250 g", PULVER));
		list.add(product("Kreatin Neutral", kreatinText, "42.90", 10, "kreatin-neutral.svg", kreatin, "1 kg", PULVER));
		list.add(product("Kreatin Kapseln", kreatinKapselnText, "19.90", 25, "kreatin-kapseln.svg", kreatin,
				"120 Kapseln", KAPSELN));

		// Protein: Pulver in drei Größen, Riegel und fertige Shakes
		list.add(product("Whey Protein Schoko", wheyText, "34.90", 60, "whey-schoko.svg", protein, "1 kg", PULVER));
		list.add(product("Whey Protein Vanille", wheyText, "34.90", 35, "whey-vanille.svg", protein, "1 kg", PULVER));
		list.add(product("Whey Protein Erdbeere", wheyText, "34.90", 0, "whey-erdbeere.svg", protein, "1 kg", PULVER));
		list.add(product("Whey Protein Cookies", wheyText, "36.90", 12, "whey-cookies.svg", protein, "1 kg", PULVER));
		list.add(product("Whey Protein Schoko", wheyText, "19.90", 20, "whey-schoko.svg", protein, "500 g", PULVER));
		list.add(product("Whey Protein Schoko", wheyText, "74.90", 8, "whey-schoko.svg", protein, "2,5 kg", PULVER));
		list.add(product("Proteinriegel Schoko-Crisp", riegelText, "2.49", 80, "riegel-schoko.svg", protein, "45 g",
				RIEGEL));
		list.add(product("Proteinriegel Erdnuss", riegelText, "2.49", 50, "riegel-erdnuss.svg", protein, "45 g", RIEGEL));
		list.add(product("Proteinriegel Schoko-Crisp", riegelText, "26.90", 15, "riegel-schoko.svg", protein,
				"12 × 45 g", RIEGEL));
		list.add(product("Proteinshake Vanille", shakeText, "2.99", 40, "shake-vanille.svg", protein, "330 ml",
				GETRAENK));
		list.add(product("Proteinshake Schoko", shakeText, "2.99", 3, "shake-schoko.svg", protein, "330 ml", GETRAENK));

		// Aminosäuren: Pulver in zwei Größen und als Getränk
		list.add(product("EAA Mango", eaaText, "27.90", 22, "eaa-mango.svg", amino, "500 g", PULVER));
		list.add(product("EAA Eistee Pfirsich", eaaText, "27.90", 3, "eaa-eistee-pfirsich.svg", amino, "500 g", PULVER));
		list.add(product("EAA Cola", eaaText, "27.90", 15, "eaa-cola.svg", amino, "500 g", PULVER));
		list.add(product("EAA Mango", eaaText, "49.90", 6, "eaa-mango.svg", amino, "1 kg", PULVER));
		list.add(product("EAA Drink Zitrone", eaaDrinkText, "2.79", 30, "eaa-drink-zitrone.svg", amino, "500 ml",
				GETRAENK));

		// Hyrox-Supplement: Dose und Portionsbeutel
		list.add(product("Hyrox-Supplement Orange", hyroxText, "39.90", 25, "hyrox-orange.svg", hyrox, "500 g", PULVER));
		list.add(product("Hyrox-Supplement Beere", hyroxText, "39.90", 9, "hyrox-beere.svg", hyrox, "500 g", PULVER));
		list.add(product("Hyrox-Supplement Zitrone-Limette", hyroxText, "39.90", 2, "hyrox-zitrone-limette.svg", hyrox,
				"500 g", PULVER));
		list.add(product("Hyrox-Supplement Orange", hyroxText, "29.90", 14, "hyrox-orange.svg", hyrox,
				"20 × 20 g Beutel", PULVER));

		// Energie & Elektrolyte: für den Wettkampftag
		list.add(product("Elektrolyt-Drink Zitrone", elektrolytText, "2.49", 45, "elektrolyt-zitrone.svg", energie,
				"500 ml", GETRAENK));
		list.add(product("Elektrolyt-Tabs Orange", tabsText, "6.90", 0, "elektrolyt-tabs.svg", energie, "20 Tabletten",
				KAPSELN));
		list.add(product("Energy-Gel Cola", gelText, "1.99", 60, "gel-cola.svg", energie, "60 ml", GEL));
		list.add(product("Energy-Gel Cola", gelText, "21.90", 12, "gel-cola.svg", energie, "12 × 60 ml", GEL));
		list.add(product("Energieriegel Banane", energieRiegelText, "2.19", 35, "riegel-banane.svg", energie, "50 g",
				RIEGEL));
		return list;
	}

	/** Ein paar Produkte im Sale (G1, 0023), damit der Sale-Bereich gleich etwas zeigt. Zeitraum ab heute. */
	private static void startSampleSales(List<Product> list) {
		LocalDate today = ShopTime.today();
		sale(list, "Whey Protein Vanille", "1 kg", "27.90", today, today.plusDays(14));
		sale(list, "EAA Cola", "500 g", "22.90", today, today.plusDays(7));
		sale(list, "Proteinriegel Schoko-Crisp", "12 × 45 g", "21.90", today, today.plusDays(30));
		sale(list, "Hyrox-Supplement Beere", "500 g", "33.90", today, today.plusDays(3));
	}

	private static void sale(List<Product> list, String name, String size, String salePrice, LocalDate from,
			LocalDate until) {
		list.stream()
				.filter(p -> p.getName().equals(name) && size.equals(p.getSize()))
				.findFirst()
				.ifPresent(p -> p.startSale(new BigDecimal(salePrice), from, until));
	}

	/**
	 * Barcodes für die Nährwerte aus Open Food Facts (H1, 0027). Unser Shop ist ausgedacht, deshalb steht
	 * hier der Barcode eines echten, vergleichbaren Produkts (Sportness Protein 90 Shake Schoko, mit
	 * Nährwerten "wie verkauft" bei Open Food Facts); die Seite nennt Name und Marke dazu.
	 */
	private static void addSampleBarcodes(List<Product> list) {
		list.stream()
				.filter(p -> p.getName().equals("Whey Protein Schoko"))
				.forEach(p -> p.setEan("4066447398083"));
	}

	/** Rabattcodes zum Ausprobieren (G2, 0024): einer gültig, einer begrenzt, einer abgelaufen. */
	private void createSampleDiscountCodes() {
		LocalDate today = ShopTime.today();
		discountCodes.save(new DiscountCode("HYROX10", 10, today.minusDays(1), today.plusDays(60), null));
		discountCodes.save(new DiscountCode("WILLKOMMEN15", 15, today.minusDays(1), today.plusDays(30), 100));
		discountCodes.save(new DiscountCode("SOMMER20", 20, today.minusDays(120), today.minusDays(60), null));
	}

	private static Product product(String name, String description, String price, int stock, String image,
			Category category, String size, ProductForm form) {
		return new Product(name, description, new BigDecimal(price), stock, "/images/" + image, category, size, form);
	}
}
