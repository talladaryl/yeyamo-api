package com.yeyamo.foundation.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DomainStandardsTest {
    @Test
    void normalizesSupportedStandards() {
        assertEquals("CM", new CountryReference("cm").countryCode());
        assertEquals("fr-CM", new LanguageReference("fr-cm").languageCode());
        assertEquals("XAF", new Money(new BigDecimal("1000"), "xaf").currencyCode());
    }

    @Test
    void rejectsInvalidStandardCodes() {
        assertThrows(IllegalArgumentException.class, () -> new CountryReference("XX"));
        assertThrows(IllegalArgumentException.class, () -> new LanguageReference("-invalid"));
        assertThrows(IllegalArgumentException.class, () -> new Money(BigDecimal.ONE, "ZZZ"));
    }

    @Test
    void validatesCoordinatesAsAPair() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeoReference("CM", null, null, null, null, 91D, 12D));
        assertThrows(IllegalArgumentException.class,
                () -> new GeoReference("CM", null, null, null, null, 4D, null));
    }
}
