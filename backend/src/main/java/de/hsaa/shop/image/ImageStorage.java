package de.hsaa.shop.image;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import de.hsaa.shop.common.InvalidFieldException;
import de.hsaa.shop.common.NotFoundException;

/**
 * Speichert hochgeladene Produktbilder als Dateien (Docker-Volume "images"), nicht in der
 * Datenbank. Die API liefert nur die Adresse /api/images/{name} (D2, ADR-0007).
 */
@Service
public class ImageStorage {

	static final String URL_PREFIX = "/api/images/";

	private static final Logger log = LoggerFactory.getLogger(ImageStorage.class);

	/** Erlaubte Formate. SVG ist bewusst nicht dabei, weil SVG-Dateien Skripte enthalten können. */
	private static final Map<String, String> EXTENSIONS = Map.of(
			"image/png", ".png",
			"image/jpeg", ".jpg",
			"image/webp", ".webp",
			"image/gif", ".gif");

	/** Nur Namen, die wir selbst vergeben haben: verhindert Pfade wie "../../etc/passwd". */
	private static final Pattern STORED_NAME = Pattern.compile("^[a-f0-9-]{36}\\.(png|jpg|webp|gif)$");

	private final Path directory;

	public ImageStorage(@Value("${shop.images.dir:./data/images}") String directory) throws IOException {
		this.directory = Path.of(directory).toAbsolutePath().normalize();
		Files.createDirectories(this.directory);
	}

	/** Speichert die Datei unter einem zufälligen Namen und liefert die URL. */
	public String store(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidFieldException("file", "Bitte ein Bild auswählen.");
		}
		String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
		String extension = EXTENSIONS.get(contentType);
		if (extension == null) {
			throw new InvalidFieldException("file", "Bitte ein Bild im Format PNG, JPG, WebP oder GIF hochladen.");
		}
		String name = UUID.randomUUID() + extension;
		try (InputStream in = file.getInputStream()) {
			Files.copy(in, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Bild konnte nicht gespeichert werden", ex);
		}
		return URL_PREFIX + name;
	}

	public Resource load(String name) {
		if (!STORED_NAME.matcher(name).matches()) {
			throw new NotFoundException("Dieses Bild gibt es nicht.");
		}
		Path path = directory.resolve(name);
		if (!Files.isRegularFile(path)) {
			throw new NotFoundException("Dieses Bild gibt es nicht.");
		}
		return new FileSystemResource(path);
	}

	/** Löscht ein früher hochgeladenes Bild. Fremde URLs und mitgelieferte Bilder bleiben unberührt. */
	public void deleteIfStored(String url) {
		if (url == null || !url.startsWith(URL_PREFIX)) {
			return;
		}
		String name = url.substring(URL_PREFIX.length());
		if (!STORED_NAME.matcher(name).matches()) {
			return;
		}
		try {
			Files.deleteIfExists(directory.resolve(name));
		}
		catch (IOException ex) {
			log.warn("Altes Bild {} konnte nicht gelöscht werden", name, ex);
		}
	}
}
