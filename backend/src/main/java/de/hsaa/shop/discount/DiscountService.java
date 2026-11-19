package de.hsaa.shop.discount;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.hsaa.shop.common.ConflictException;
import de.hsaa.shop.common.InvalidFieldException;
import de.hsaa.shop.common.NotFoundException;
import de.hsaa.shop.common.ShopTime;
import de.hsaa.shop.discount.DiscountDtos.DiscountCheckResponse;
import de.hsaa.shop.discount.DiscountDtos.DiscountCodePatch;
import de.hsaa.shop.discount.DiscountDtos.DiscountCodeRequest;
import de.hsaa.shop.discount.DiscountDtos.DiscountCodeResponse;

/** Rabattcodes anlegen, prüfen und beim Bestellen einlösen (Sprint 3, G2, docs/anforderungen/0024). */
@Service
@Transactional
public class DiscountService {

	private final DiscountCodeRepository codes;

	public DiscountService(DiscountCodeRepository codes) {
		this.codes = codes;
	}

	// ---- Admin ----

	@Transactional(readOnly = true)
	public List<DiscountCodeResponse> list() {
		LocalDate today = ShopTime.today();
		return codes.findAllByOrderByCreatedAtDesc().stream().map(c -> DiscountCodeResponse.from(c, today)).toList();
	}

	public DiscountCodeResponse create(DiscountCodeRequest request) {
		String code = normalize(request.code());
		if (request.validUntil().isBefore(request.validFrom())) {
			throw new InvalidFieldException("validUntil", "Das Ende darf nicht vor dem Beginn liegen.");
		}
		if (codes.existsByCode(code)) {
			throw new ConflictException("Den Code " + code + " gibt es schon.");
		}
		DiscountCode saved = codes.save(new DiscountCode(code, request.percent(), request.validFrom(),
				request.validUntil(), request.maxUses()));
		return DiscountCodeResponse.from(saved, ShopTime.today());
	}

	public DiscountCodeResponse patch(Long id, DiscountCodePatch patch) {
		DiscountCode code = codes.findById(id).orElseThrow(() -> new NotFoundException("Diesen Rabattcode gibt es nicht."));
		if (patch.validUntil() != null) {
			if (patch.validUntil().isBefore(code.getValidFrom())) {
				throw new InvalidFieldException("validUntil", "Das Ende darf nicht vor dem Beginn liegen.");
			}
			code.setValidUntil(patch.validUntil());
		}
		if (patch.active() != null) {
			code.setActive(patch.active());
		}
		return DiscountCodeResponse.from(code, ShopTime.today());
	}

	/** Löschen nur, solange niemand den Code benutzt hat; sonst stehen Bestellungen mit einem Code ohne Herkunft da. */
	public void delete(Long id) {
		DiscountCode code = codes.findById(id).orElseThrow(() -> new NotFoundException("Diesen Rabattcode gibt es nicht."));
		if (code.getTimesUsed() > 0) {
			throw new ConflictException("Dieser Code wurde schon eingelöst und kann nur noch deaktiviert werden.");
		}
		codes.delete(code);
	}

	// ---- Kunden ----

	/** Vorab prüfen, ob ein Code gilt, damit der Warenkorb den Rabatt anzeigen kann. */
	@Transactional(readOnly = true)
	public DiscountCheckResponse check(String rawCode) {
		DiscountCode code = codes.findByCode(normalize(rawCode))
				.orElseThrow(() -> new NotFoundException("Diesen Rabattcode gibt es nicht."));
		String problem = code.problem(ShopTime.today());
		if (problem != null) {
			throw new ConflictException(problem);
		}
		return new DiscountCheckResponse(code.getCode(), code.getPercent(), code.getValidUntil());
	}

	/**
	 * Beim Bestellen einlösen: Code sperren, prüfen, Zähler erhöhen. Ungültig → Feldfehler (400),
	 * die Bestellung wird dann gar nicht erst angelegt (alles oder nichts, wie ADR-0006).
	 */
	public DiscountCode redeem(String rawCode) {
		DiscountCode code = codes.findByCodeForUpdate(normalize(rawCode))
				.orElseThrow(() -> new InvalidFieldException("discountCode", "Diesen Rabattcode gibt es nicht."));
		String problem = code.problem(ShopTime.today());
		if (problem != null) {
			throw new InvalidFieldException("discountCode", problem);
		}
		code.use();
		return code;
	}

	static String normalize(String code) {
		return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
	}
}
