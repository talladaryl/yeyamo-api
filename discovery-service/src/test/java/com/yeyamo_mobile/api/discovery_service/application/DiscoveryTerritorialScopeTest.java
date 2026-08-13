package com.yeyamo_mobile.api.discovery_service.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.discovery_service.application.port.*;

class DiscoveryTerritorialScopeTest {
    @Test
    void localScope_requiresCountryAndCity() {
        DiscoveryQueryService service = new DiscoveryQueryService(mock(DiscoverySearchPort.class),
                mock(DiscoveryCachePort.class), mock(AdInjectionService.class));
        DiscoverySearch search = new DiscoverySearch(null, null, null, null, null, null, null,
                0, 20, false, "CM", null, null, "fr", null, null, null, null, null,
                Set.of(), DiscoveryScope.LOCAL);
        assertThrows(IllegalArgumentException.class, () -> service.search(search));
    }
}
