package de.hsaa.shop.product;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StockStatusTests {

	@Test
	void zeroIsSoldOut() {
		assertEquals(StockStatus.SOLD_OUT, StockStatus.of(0));
	}

	@Test
	void oneToFiveIsLowStock() {
		assertEquals(StockStatus.LOW_STOCK, StockStatus.of(1));
		assertEquals(StockStatus.LOW_STOCK, StockStatus.of(5));
	}

	@Test
	void moreThanFiveIsInStock() {
		assertEquals(StockStatus.IN_STOCK, StockStatus.of(6));
		assertEquals(StockStatus.IN_STOCK, StockStatus.of(1000));
	}
}
