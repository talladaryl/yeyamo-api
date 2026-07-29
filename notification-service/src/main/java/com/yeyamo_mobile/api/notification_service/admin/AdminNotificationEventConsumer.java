package com.yeyamo_mobile.api.notification_service.admin;
import java.util.*;import com.fasterxml.jackson.databind.*;import org.springframework.kafka.annotation.KafkaListener;import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.Transactional;
@Component public class AdminNotificationEventConsumer{
 private final ObjectMapper mapper;private final AdminNotificationService service;public AdminNotificationEventConsumer(ObjectMapper m,AdminNotificationService s){mapper=m;service=s;}
 @KafkaListener(topicPattern="${admin.notifications.topic-pattern:partner-events|moderation\\.events|payment\\.events|booking\\.events|campaign\\.events|support\\.events|system\\.events|security\\.events}",groupId="${spring.kafka.consumer.group-id:notification-service}-admin")@Transactional public void consume(String raw)throws Exception{JsonNode e=mapper.readTree(raw);Mapping m=map(text(e,"eventType"),e.path("payload"));if(m==null)return;service.create(UUID.fromString(required(e,"eventId")),m.type,m.title,m.message,m.resourceType,m.resourceId,null,m.role);}
 private Mapping map(String type,JsonNode payload){
  String resourceId=first(payload,"partnerId","reportId","paymentId","bookingId","campaignId","conversationId","alertId");
  if(resourceId==null)return null;
  return switch(type){
   case "partner.submitted" -> new Mapping("KYC","Nouveau dossier KYC","Un partenaire a soumis son dossier.","PARTNER",resourceId,"ADMIN");
   case "moderation.report.critical" -> new Mapping("MODERATION","Signalement critique","Un signalement critique requiert une revue.","REPORT",resourceId,"MODERATOR");
   case "payment.anomaly.detected" -> new Mapping("PAYMENT","Anomalie de paiement","Une anomalie financière a été détectée.","PAYMENT",resourceId,"ADMIN");
   case "booking.incident" -> new Mapping("BOOKING","Incident de réservation","Une réservation nécessite une intervention.","BOOKING",resourceId,"SUPPORT");
   case "campaign.pending_approval" -> new Mapping("CAMPAIGN","Campagne en attente","Une campagne attend une approbation.","CAMPAIGN",resourceId,"ADMIN");
   case "support.escalated" -> new Mapping("SUPPORT","Conversation escaladée","Une conversation support a été escaladée.","SUPPORT_CONVERSATION",resourceId,"SUPPORT");
   case "system.alert" -> new Mapping("SYSTEM","Alerte système","Une alerte système nécessite une vérification.","SYSTEM_ALERT",resourceId,"SUPER_ADMIN");
   case "security.alert" -> new Mapping("SECURITY","Alerte de sécurité","Une alerte de sécurité requiert une action.","SECURITY_ALERT",resourceId,"SUPER_ADMIN");
   default -> null;
  };
 }
 private String first(JsonNode p,String...keys){for(String k:keys){String v=text(p,k);if(v!=null&&!v.isBlank())return v;}return null;}private String required(JsonNode n,String f){String v=text(n,f);if(v==null)throw new IllegalArgumentException(f+" is required");return v;}private String text(JsonNode n,String f){JsonNode v=n.get(f);return v==null||v.isNull()?null:v.asText();}private record Mapping(String type,String title,String message,String resourceType,String resourceId,String role){}
}
