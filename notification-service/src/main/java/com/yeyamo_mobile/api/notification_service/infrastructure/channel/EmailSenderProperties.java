package com.yeyamo_mobile.api.notification_service.infrastructure.channel;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="notification.email") public record EmailSenderProperties(boolean deliveryEnabled,String fromAddress,String fromName){public EmailSenderProperties{fromName=fromName==null||fromName.isBlank()?"YeYamo":fromName;if(deliveryEnabled&&(fromAddress==null||fromAddress.isBlank()))throw new IllegalStateException("EMAIL_FROM_ADDRESS is required when email delivery is enabled");}}
