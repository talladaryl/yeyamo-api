package com.yeyamo_mobile.api.admin_service.governance;
import static org.junit.jupiter.api.Assertions.*;import java.lang.reflect.Method;import org.junit.jupiter.api.Test;import org.springframework.cache.annotation.CacheEvict;
class GovernanceSecurityTest {
 @Test void rejectsSecretLikeValues(){assertThrows(IllegalArgumentException.class,()->SettingDefinition.PLATFORM_NAME.validate("apiKey=very-secret-value"));}
 @Test void mutationsInvalidateTheirCaches()throws Exception{Method setting=GovernanceService.class.getMethod("updateSetting",String.class,GovernanceDtos.SettingUpdateRequest.class,jakarta.servlet.http.HttpServletRequest.class);Method flag=GovernanceService.class.getMethod("updateFlag",String.class,GovernanceDtos.FeatureFlagRequest.class,jakarta.servlet.http.HttpServletRequest.class);assertEquals("platformSettings",setting.getAnnotation(CacheEvict.class).cacheNames()[0]);assertEquals("featureFlags",flag.getAnnotation(CacheEvict.class).cacheNames()[0]);}
 @Test void allowlistRejectsUnknownKeys(){assertThrows(IllegalArgumentException.class,()->SettingDefinition.require("smtp_password"));}
}
