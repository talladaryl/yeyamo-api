package com.yeyamo_mobile.api.payment_service.application;

import java.math.BigDecimal;
import java.util.*;

/** Resolves only an explicitly configured provider; no currency conversion or implicit fallback. */
public interface PaymentProviderResolver {
    record Request(String countryCode, String currencyCode, PaymentMethod paymentMethod,
            String operationType, BigDecimal amount) { }
    record ResolvedProvider(String providerCode, String configurationReference) { }
    Optional<ResolvedProvider> resolve(Request request);
}
