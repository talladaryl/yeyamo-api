package com.yeyamo_mobile.api.messaging_service.infrastructure.websocket;
import java.util.*;import org.springframework.beans.factory.annotation.Qualifier;import org.springframework.beans.factory.annotation.Value;import org.springframework.context.annotation.*;import org.springframework.messaging.*;import org.springframework.messaging.simp.config.MessageBrokerRegistry;import org.springframework.messaging.simp.config.ChannelRegistration;import org.springframework.messaging.simp.stomp.*;import org.springframework.messaging.support.ChannelInterceptor;import org.springframework.messaging.support.MessageHeaderAccessor;import org.springframework.scheduling.TaskScheduler;import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.core.authority.SimpleGrantedAuthority;import org.springframework.security.oauth2.jwt.JwtDecoder;import org.springframework.web.socket.config.annotation.*;
@Configuration @EnableWebSocketMessageBroker public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{
 private final JwtDecoder decoder;
 private final WebSocketAuthorizationService authorizationService;
 private final WebSocketRateLimiter rateLimiter;
 private final long clientHeartbeatMs;
 private final long serverHeartbeatMs;
 private final String[] allowedOriginPatterns;
 private final TaskScheduler messageBrokerTaskScheduler;
 
 public WebSocketConfig(JwtDecoder decoder, 
                        WebSocketAuthorizationService authorizationService,
                        WebSocketRateLimiter rateLimiter,
                        @Qualifier("yeyamoStompTaskScheduler") TaskScheduler messageBrokerTaskScheduler,
                        @Value("${websocket.heartbeat.client-ms:10000}") long clientHeartbeatMs,
                        @Value("${websocket.heartbeat.server-ms:10000}") long serverHeartbeatMs,
                        @Value("${websocket.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
                        String allowedOriginPatterns){
  this.decoder=decoder;
  this.authorizationService=authorizationService;
  this.rateLimiter=rateLimiter;
  this.messageBrokerTaskScheduler=messageBrokerTaskScheduler;
  this.clientHeartbeatMs=positive(clientHeartbeatMs,"websocket.heartbeat.client-ms");
  this.serverHeartbeatMs=positive(serverHeartbeatMs,"websocket.heartbeat.server-ms");
  this.allowedOriginPatterns=Arrays.stream(allowedOriginPatterns.split(","))
   .map(String::trim).filter(value->!value.isBlank()).toArray(String[]::new);
  if(this.allowedOriginPatterns.length==0)throw new IllegalArgumentException("At least one WebSocket origin is required");
 }
 
 public void registerStompEndpoints(StompEndpointRegistry registry){registry.addEndpoint("/ws/messaging").setAllowedOriginPatterns(allowedOriginPatterns);}
 
 public void configureMessageBroker(MessageBrokerRegistry registry){
  registry.enableSimpleBroker("/queue")
   .setTaskScheduler(messageBrokerTaskScheduler)
   .setHeartbeatValue(new long[]{serverHeartbeatMs, clientHeartbeatMs});
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
 private static long positive(long value,String name){if(value<=0)throw new IllegalArgumentException(name+" must be positive");return value;}
 static final class MessagingException extends RuntimeException{MessagingException(String message){super(message);}}
}
