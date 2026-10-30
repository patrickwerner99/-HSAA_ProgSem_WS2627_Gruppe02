package de.hsaa.shop.image;

import java.time.Duration;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import de.hsaa.shop.product.ProductResponse;
import de.hsaa.shop.product.ProductService;

/**
 * Produktbilder (docs/anforderungen/0014, D1–D3).
 *
 * <pre>
 * POST   /api/products/{id}/image   Bild hochladen (multipart, Feld "file"), nur ADMIN   200 / 400 / 401 / 403 / 404 / 413
 * DELETE /api/products/{id}/image   Bild entfernen, nur ADMIN                             200 / 401 / 403 / 404
 * GET    /api/images/{name}         Bilddatei, für alle                                   200 / 404
 * </pre>
 */
@RestController
public class ImageController {

	private final ImageStorage storage;
	private final ProductService products;

	public ImageController(ImageStorage storage, ProductService products) {
		this.storage = storage;
		this.products = products;
	}

	@PostMapping(path = "/api/products/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ProductResponse upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
		products.findById(id); // 404, bevor eine Datei gespeichert wird
		String url = storage.store(file);
		String old = products.replaceImage(id, url);
		storage.deleteIfStored(old);
		return products.findById(id);
	}

	@DeleteMapping("/api/products/{id}/image")
	public ProductResponse remove(@PathVariable Long id) {
		String old = products.replaceImage(id, null);
		storage.deleteIfStored(old);
		return products.findById(id);
	}

	@GetMapping("/api/images/{name}")
	public ResponseEntity<Resource> get(@PathVariable String name) {
		Resource image = storage.load(name);
		MediaType type = MediaTypeFactory.getMediaType(name).orElse(MediaType.APPLICATION_OCTET_STREAM);
		// Der Dateiname ist zufällig und ändert sich bei jedem Upload, darum darf der Browser lange cachen.
		return ResponseEntity.ok()
				.contentType(type)
				.cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
				.body(image);
	}
}
