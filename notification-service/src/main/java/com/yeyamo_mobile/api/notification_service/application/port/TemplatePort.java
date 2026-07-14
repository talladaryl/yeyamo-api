package com.yeyamo_mobile.api.notification_service.application.port;
import java.util.Map;import com.yeyamo_mobile.api.notification_service.domain.*;
public interface TemplatePort{RenderedTemplate render(String eventType,NotificationChannel channel,String locale,Map<String,String>variables);}
