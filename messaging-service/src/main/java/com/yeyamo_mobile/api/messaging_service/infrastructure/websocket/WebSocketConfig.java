package com.yeyamo_mobile.api.messaging_service.infrastructure.websocket;
import java.util.*;import org.springframework.context.annotation.*;import org.springframework.messaging.*;import org.springframework.messaging.simp.config.MessageBrokerRegistry;import org.springframework.messaging.simp.config.ChannelRegistration;import org.springframework.messaging.simp.stomp.*;import org.springframework.messaging.support.ChannelInterceptor;import org.springframework.messaging.support.MessageHeaderAccessor;import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.core.authority.SimpleGrantedAuthority;import org.springframework.security.oauth2.jwt.JwtDecoder;import org.springframework.web.socket.config.annotation.*;
@Configuration @EnableWebSocketMessageBroker public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{
 private final JwtDecoder decoder;
 private final WebSocketAuthorizationService authorizationService;
 private final WebSocketRateLimiter rateLimiter;
 
 public WebSocketConfig(JwtDecoder decoder, 
                        WebSocketAuthorizationService authorizationService,
                        WebSocketRateLimiter rateLimiter){
  this.decoder=decoder;
  this.authorizationService=authorizationService;
  this.rateLimiter=rateLimiter;
 }
 
 public void registerStompEndpoints(StompEndpointRegistry registry){registry.addEndpoint("/ws/messaging").setAllowedOriginPatterns("*");}
 
 public void configureMessageBroker(MessageBrokerRegistry registry){
  registry.enableSimpleBroker("/queue")
   .setHeartbeatValue(new long[]{10000, 10000}); // 10s heartbeat client/server
  registry.setUserDestinationPrefix("/user");
  registry.setApplicationDestinationPrefixes("/app");
 }
 public void configureClientInboundChannel(ChannelRegistration registration){
  registration.interceptors(new ChannelInterceptor(){
   public Message<?>preSend(Message<?>message,org.springframework.messaging.MessageChannel channel){
    StompHeaderAccessor accessor=MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);
    if(accessor==null)return message;
    
    // ─── AUTHENTIFICATION JWT (CONNECT) ─────────────────────────────────────
    if(StompCommand.CONNECT.equals(accessor.getCommand())){
     String value=accessor.getFirstNativeHeader("Authorization");
     if(value==null||!value.startsWith("Bearer "))
      throw new MessagingException("Unauthorized");
     
     var jwt=decoder.decode(value.substring(7));
     List<String>roles=new ArrayList<>();
     List<String>many=jwt.getClaimAsStringList("roles");
     if(many!=null)roles.addAll(many);
     String one=jwt.getClaimAsString("role");
     if(one!=null)roles.add(one);
     var authorities=roles.stream().map(r->new SimpleGrantedAuthority(r.startsWith("ROLE_")?r:"ROLE_"+r)).toList();
     accessor.setUser(new UsernamePasswordAuthenticationToken(jwt.getSubject(),jwt,authorities));
    }
    
    // ─── AUTORISATION SOUSCRIPTION (SUBSCRIBE) ──────────────────────────────
    if(StompCommand.SUBSCRIBE.equals(accessor.getCommand())){
     String destination=String.valueOf(accessor.getDestination());
     
     // Vérification 1: Seulement destinations /user/queue/* autorisées
     if(!destination.startsWith("/user/queue/"))
      throw new MessagingException("Unauthorized");
     
     // Vérification 2: Rate limiting (protection énumération)
     String userId=accessor.getUser()!=null?accessor.getUser().getName():null;
     if(userId==null)
      throw new MessagingException("Unauthorized");
     
     if(!rateLimiter.tryAcquire(userId))
      throw new MessagingException("Unauthorized"); // Message générique (pas "rate limit exceeded")
     
     // Vérification 3: Autorisation conversation-level
     if(!authorizationService.isAuthorizedToSubscribe(userId,destination))
      throw new MessagingException("Unauthorized"); // Message générique (pas de fuite d'info)
    }
    
    return message;
   }
  });
 }
 static final class MessagingException extends RuntimeException{MessagingException(String message){super(message);}}
}
