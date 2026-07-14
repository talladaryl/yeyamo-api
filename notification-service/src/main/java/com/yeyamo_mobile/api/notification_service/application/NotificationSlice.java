package com.yeyamo_mobile.api.notification_service.application;
import java.util.List;import com.yeyamo_mobile.api.notification_service.domain.Notification;
public record NotificationSlice(int page,int size,boolean hasNext,List<Notification>items){}
