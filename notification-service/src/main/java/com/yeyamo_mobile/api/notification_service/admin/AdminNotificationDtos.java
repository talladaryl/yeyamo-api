package com.yeyamo_mobile.api.notification_service.admin;
import java.time.Instant;import java.util.UUID;
public final class AdminNotificationDtos{private AdminNotificationDtos(){}public record Response(UUID id,String type,String title,String message,String resourceType,String resourceId,Instant readAt,Instant createdAt){}public record UnreadCount(long count){}public record Updated(int updated){}}
