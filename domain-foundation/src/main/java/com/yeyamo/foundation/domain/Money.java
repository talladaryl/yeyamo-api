package com.yeyamo.foundation.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public record Money(BigDecimal amount, String currencyCode) {
    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        currencyCode = Standards.currencyCode(currencyCode);
        int fractionDigits = Currency.getInstance(currencyCode).getDefaultFractionDigits();
        if (fractionDigits >= 0 && amount.scale() > fractionDigits) {
            throw new IllegalArgumentException("amount has too many fractional digits for " + currencyCode);
        }
        amount = amount.setScale(Math.max(fractionDigits, 0), RoundingMode.UNNECESSARY);
    }
}
