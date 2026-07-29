package com.yeyamo_mobile.api.notification_service.application.port;
import com.yeyamo_mobile.api.notification_service.domain.*;
public interface ChannelSender{NotificationChannel channel();void send(Delivery delivery,Notification notification);}
