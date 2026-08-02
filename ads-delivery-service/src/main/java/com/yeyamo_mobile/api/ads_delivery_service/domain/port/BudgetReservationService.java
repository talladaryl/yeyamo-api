package com.yeyamo_mobile.api.ads_delivery_service.domain.port;

import java.math.BigDecimal;

public interface BudgetReservationService {
    boolean reserveBudget(String campaignId, BigDecimal amount, String reservationId);
    void confirmReservation(String reservationId, BigDecimal actualAmount);
    void releaseReservation(String reservationId);
}
