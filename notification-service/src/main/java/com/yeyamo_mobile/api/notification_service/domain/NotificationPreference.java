package com.yeyamo_mobile.api.notification_service.domain;
import java.time.Instant;
public record NotificationPreference(String userId,boolean inAppEnabled,boolean emailEnabled,boolean pushEnabled,String emailAddress,String pushToken,String locale,Instant updatedAt){
 public NotificationPreference{if(userId==null||userId.isBlank())throw new IllegalArgumentException("userId is required");locale=locale==null||locale.isBlank()?"fr":locale.toLowerCase();if(emailEnabled&&(emailAddress==null||emailAddress.isBlank()))throw new IllegalArgumentException("emailAddress is required when email is enabled");if(pushEnabled&&(pushToken==null||pushToken.isBlank()))throw new IllegalArgumentException("pushToken is required when push is enabled");}
 public static NotificationPreference defaults(String userId,String email){return new NotificationPreference(userId,true,email!=null&&!email.isBlank(),false,email,null,"fr",Instant.now());}
 public boolean enabled(NotificationChannel c){return switch(c){case IN_APP->inAppEnabled;case EMAIL->emailEnabled;case PUSH->pushEnabled;};}
 public String destination(NotificationChannel c){return switch(c){case IN_APP->userId;case EMAIL->emailAddress;case PUSH->pushToken;};}
}
