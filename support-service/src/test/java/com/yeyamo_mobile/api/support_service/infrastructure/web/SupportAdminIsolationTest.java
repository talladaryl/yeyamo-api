package com.yeyamo_mobile.api.support_service.infrastructure.web;
import static org.junit.jupiter.api.Assertions.*;import java.util.Arrays;import org.junit.jupiter.api.Test;import org.springframework.web.bind.annotation.RequestMapping;
class SupportAdminIsolationTest {
 @Test void controllerIsExposedOnlyUnderAdminNamespace(){RequestMapping mapping=SupportAdminController.class.getAnnotation(RequestMapping.class);assertNotNull(mapping);assertArrayEquals(new String[]{"/api/v1/admin/support/conversations"},mapping.value());assertTrue(Arrays.stream(SupportAdminController.class.getMethods()).noneMatch(m->m.getName().toLowerCase().contains("usernote")));}
}
