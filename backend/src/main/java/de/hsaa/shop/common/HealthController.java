package de.hsaa.shop.common;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Läufst du?" für den Docker-Healthcheck. nginx startet erst, wenn das Backend hier antwortet
 * (siehe docker-compose.yml).
 */
@RestController
public class HealthController {

	@GetMapping("/api/health")
	public Map<String, String> health() {
		return Map.of("status", "UP");
	}
}
