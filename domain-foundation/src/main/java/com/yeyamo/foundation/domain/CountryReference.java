package com.yeyamo.foundation.domain;

public record CountryReference(String countryCode) {
    public CountryReference {
        countryCode = Standards.countryCode(countryCode);
    }
}
