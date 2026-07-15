package com.yeyamo_mobile.api.place_service.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;import org.springframework.mock.web.*;

class LegacyDeprecationFilterTests {
 @Test void identifiesLegacyApiAndItsCatalogSuccessor() throws Exception{
  LegacyDeprecationFilter filter=new LegacyDeprecationFilter("2027-01-31","http://catalog/api/v1/catalog/assets");
  MockHttpServletRequest request=new MockHttpServletRequest("GET","/api/v1/places/nearby");MockHttpServletResponse response=new MockHttpServletResponse();
  filter.doFilter(request,response,new MockFilterChain());
  assertEquals("true",response.getHeader("Deprecation"));assertEquals("2027-01-31",response.getHeader("Sunset"));
  assertEquals("<http://catalog/api/v1/catalog/assets>; rel=\"successor-version\"",response.getHeader("Link"));
 }
}
