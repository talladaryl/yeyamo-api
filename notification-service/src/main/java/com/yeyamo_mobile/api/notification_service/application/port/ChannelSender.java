package com.yeyamo_mobile.api.notification_service.application.port;
import com.yeyamo_mobile.api.notification_service.domain.NotificationChannel;
public interface ChannelSender{NotificationChannel channel();void send(String destination,String subject,String body,String dataJson);}
