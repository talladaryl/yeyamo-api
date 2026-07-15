package com.yeyamo_mobile.api.place_service.config;
import java.io.IOException;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;import org.springframework.web.filter.OncePerRequestFilter;import jakarta.servlet.*;import jakarta.servlet.http.*;
@Component public class LegacyDeprecationFilter extends OncePerRequestFilter{
 private final String sunset;private final String successor;
 public LegacyDeprecationFilter(@Value("${yeyamo.place.deprecation.sunset:2027-01-31}")String sunset,@Value("${yeyamo.place.deprecation.successor:http://localhost:8088/api/v1/catalog/assets}")String successor){this.sunset=sunset;this.successor=successor;}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  if(request.getRequestURI().startsWith("/api/v1/")){response.setHeader("Deprecation","true");response.setHeader("Sunset",sunset);response.setHeader("Link","<"+successor+">; rel=\"successor-version\"");}
  chain.doFilter(request,response);
 }
}
