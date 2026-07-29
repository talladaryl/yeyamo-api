package com.yeyamo_mobile.api.moderation_trust_service.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.SanctionType;
import com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence.SanctionEntity;

class SanctionEntityTest {
    @Test void temporarySuspensionRequiresEndDate(){
        assertThrows(IllegalArgumentException.class,()->SanctionEntity.create("user-1",SanctionType.TEMPORARY_SUSPENSION,"abuse",Instant.now(),null,"admin-1",null));
    }
    @Test void permanentSuspensionHasAuditableActor(){
        SanctionEntity sanction=SanctionEntity.create("user-1",SanctionType.PERMANENT_SUSPENSION,"repeated abuse",null,null,"admin-1",null);
        assertEquals("admin-1",sanction.getActorId());assertEquals("user-1",sanction.getSubjectId());
    }
}
