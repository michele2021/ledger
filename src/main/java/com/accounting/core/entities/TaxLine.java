package com.accounting.core.entities;

import java.math.BigDecimal;

public record TaxLine(String price, BigDecimal rate, String title) {
}
