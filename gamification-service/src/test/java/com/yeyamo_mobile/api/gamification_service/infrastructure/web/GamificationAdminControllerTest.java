package com.yeyamo_mobile.api.gamification_service.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.yeyamo_mobile.api.gamification_service.infrastructure.persistence.XpLedgerRepository;

class GamificationAdminControllerTest {
    @Test
    void filtersLedgerByUserWithoutLoadingAllEntries() {
        XpLedgerRepository repository = mock(XpLedgerRepository.class);
        PageRequest page = PageRequest.of(0, 25);
        when(repository.findByUserId("user-1", page)).thenReturn(new PageImpl<>(java.util.List.of(), page, 0));

        var result = new GamificationAdminController(repository).ledger("user-1", page);

        assertThat(result).isEmpty();
        verify(repository).findByUserId("user-1", page);
    }
}
