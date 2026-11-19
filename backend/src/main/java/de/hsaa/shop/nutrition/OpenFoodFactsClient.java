package de.hsaa.shop.nutrition;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.hsaa.shop.common.ExternalServiceException;

/**
 * Holt Produktdaten von der Open-Food-Facts-API (ADR-0011, ADR-0012). Kein API-Key nötig, nur ein
 * aussagekräftiger User-Agent, wie Open Food Facts es verlangt. Kurze Timeouts, damit eine langsame
 * API die Produktseite nie aufhält (H2).
 */
@Component
public class OpenFoodFactsClient {

	static final String FIELDS = "product_name,brands,nutriments,nutriscore_grade";

	private final RestClient client;
	private final String baseUrl;

	public OpenFoodFactsClient(
			@Value("${shop.openfoodfacts.base-url:https://world.openfoodfacts.org}") String baseUrl,
			@Value("${shop.openfoodfacts.timeout-ms:2500}") long timeoutMs) {
		HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
		factory.setReadTimeout(Duration.ofMillis(timeoutMs));
		this.baseUrl = baseUrl;
		this.client = RestClient.builder()
				.requestFactory(factory)
				.baseUrl(baseUrl)
				.defaultHeader("User-Agent", "HyroxShop/1.0 (Hochschule Aalen, Projektseminar)")
				.build();
	}

	/**
	 * Produkt mit diesem Barcode, oder leer, wenn Open Food Facts es nicht kennt.
	 *
	 * @throws ExternalServiceException wenn Open Food Facts nicht oder zu langsam antwortet
	 */
	public Optional<Nutrition> fetch(String ean) {
		OffResponse response;
		try {
			response = client.get()
					.uri("/api/v2/product/{ean}.json?fields={fields}", ean, FIELDS)
					.exchange((request, res) -> {
						// Unbekannter Barcode: Open Food Facts antwortet mit 404 (und "status": 0).
						if (res.getStatusCode().value() == 404) {
							return null;
						}
						if (res.getStatusCode().isError()) {
							throw new RestClientException("Open Food Facts antwortet mit " + res.getStatusCode());
						}
						return res.bodyTo(OffResponse.class);
					});
		}
		catch (RestClientException ex) {
			throw new ExternalServiceException("Die Nährwerte sind gerade nicht erreichbar. Bitte versuche es später.");
		}
		if (response == null || response.status() != 1 || response.product() == null) {
			return Optional.empty();
		}
		OffProduct p = response.product();
		Map<String, Object> n = p.nutriments() == null ? Map.of() : p.nutriments();
		Nutrition nutrition = new Nutrition(ean, p.product_name(), p.brands(),
				energyKcal(n), number(n, "proteins_100g"), number(n, "carbohydrates_100g"),
				number(n, "sugars_100g"), number(n, "fat_100g"), number(n, "saturated-fat_100g"),
				number(n, "salt_100g"), p.nutriscore_grade(),
				baseUrl + "/product/" + ean, "Open Food Facts", Instant.now(), false);
		// Produkt bekannt, aber ohne Nährwerte "wie verkauft": wie unbekannt behandeln, statt eine leere Tabelle zu zeigen.
		return nutrition.hasValues() ? Optional.of(nutrition) : Optional.empty();
	}

	/** Kalorien; manche Einträge haben nur kJ, dann umrechnen (1 kcal = 4,184 kJ). */
	private static BigDecimal energyKcal(Map<String, Object> nutriments) {
		BigDecimal kcal = number(nutriments, "energy-kcal_100g");
		if (kcal != null) {
			return kcal;
		}
		BigDecimal kj = number(nutriments, "energy-kj_100g");
		if (kj == null) {
			kj = number(nutriments, "energy_100g"); // bei Open Food Facts immer kJ
		}
		return kj == null ? null : kj.divide(new BigDecimal("4.184"), 0, java.math.RoundingMode.HALF_UP);
	}

	private static BigDecimal number(Map<String, Object> nutriments, String key) {
		Object value = nutriments.get(key);
		if (value instanceof Number number) {
			return new BigDecimal(number.toString()).stripTrailingZeros();
		}
		if (value instanceof String text && !text.isBlank()) {
			try {
				return new BigDecimal(text.trim());
			}
			catch (NumberFormatException ex) {
				return null;
			}
		}
		return null;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record OffResponse(int status, OffProduct product) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record OffProduct(String product_name, String brands, Map<String, Object> nutriments, String nutriscore_grade) {
	}
}
