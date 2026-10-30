package de.hsaa.shop.order;

import java.util.List;
import java.util.stream.Collectors;

import de.hsaa.shop.order.OrderDtos.StockProblem;

/** Nicht genug auf Lager (B2) → HTTP 409 mit der Liste der betroffenen Produkte. */
public class OutOfStockException extends RuntimeException {

	private final List<StockProblem> problems;

	public OutOfStockException(List<StockProblem> problems) {
		super(buildMessage(problems));
		this.problems = problems;
	}

	public List<StockProblem> getProblems() {
		return problems;
	}

	private static String buildMessage(List<StockProblem> problems) {
		String list = problems.stream()
				.map(p -> p.available() == 0
						? "„" + p.productName() + "“ (ausverkauft)"
						: "„" + p.productName() + "“ (nur noch " + p.available() + " verfügbar)")
				.collect(Collectors.joining(", "));
		return "Nicht genug auf Lager: " + list + ". Bitte passe deinen Warenkorb an.";
	}
}
