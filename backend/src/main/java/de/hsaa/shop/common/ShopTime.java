package de.hsaa.shop.common;

import java.time.LocalDate;
import java.time.ZoneId;

/** "Heute" im Shop. Sale-Zeiträume und Rabattcodes gelten nach deutscher Zeit, egal wo der Server läuft. */
public final class ShopTime {

	public static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

	private ShopTime() {
	}

	public static LocalDate today() {
		return LocalDate.now(ZONE);
	}
}
