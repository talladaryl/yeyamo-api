package com.yeyamo_mobile.api.mission_reward_service.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.mission_reward_service.infrastructure.persistence.*;

class AdminDefinitionsTest {
    @Test void xpRuleRejectsNonPositiveAmount(){
        assertThrows(IllegalArgumentException.class,()->XpRuleEntity.create("POST_CREATED",0,1,1,"{}"));
    }
    @Test void rewardRejectsNegativeStock(){
        assertThrows(IllegalArgumentException.class,()->RewardDefinitionEntity.create("PROMO","VOUCHER",BigDecimal.ONE,10,-1,"XAF"));
    }
    @Test void badgeStartsAsDraft(){
        assertEquals(AdminDefinitionStatus.DRAFT,BadgeDefinitionEntity.create("EXPLORER","Explorer","Visit places","{}").getStatus());
    }
}
