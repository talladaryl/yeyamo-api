package com.yeyamo_mobile.api.gamification_service.domain;import java.time.*;
public record Progress(String userId,long totalXp,int level,int currentStreak,int longestStreak,LocalDate lastActivityDate,Instant updatedAt){public static int levelFor(long xp){return 1+(int)Math.floor(Math.sqrt(Math.max(0,xp)/100d));}public long xpForNextLevel(){int next=level+1;return (long)(next-1)*(next-1)*100;}}
