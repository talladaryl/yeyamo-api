package com.yeyamo_mobile.api.admin_service.service;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;import java.util.*;import com.yeyamo_mobile.api.admin_service.models.AdminAuditLog;import com.yeyamo_mobile.api.admin_service.repository.AdminAuditLogRepository;import org.hibernate.annotations.Immutable;import org.junit.jupiter.api.Test;
class AuditServiceSecurityTest {
 @Test void recursivelyRemovesSensitiveMetadata(){var service=new AuditService(mock(AdminAuditLogRepository.class));Map<String,Object>clean=service.sanitize(Map.of("status","ok","token","secret","nested",Map.of("password","hidden","safe","yes")));assertEquals("ok",clean.get("status"));assertFalse(clean.containsKey("token"));assertEquals(Map.of("safe","yes"),clean.get("nested"));}
 @Test void auditEntityIsImmutable(){assertNotNull(AdminAuditLog.class.getAnnotation(Immutable.class));}
}
