package com.yeyamo_mobile.api.admin_service.governance;
import java.util.*;
public enum SettingDefinition {
 PLATFORM_NAME("STRING","Nom public de la plateforme",false),
 MAINTENANCE_MODE("BOOLEAN","Active le mode maintenance",true),
 DEFAULT_TIMEZONE("STRING","Fuseau horaire fonctionnel",false),
 MAX_UPLOAD_SIZE_MB("INTEGER","Taille maximale fonctionnelle des fichiers",true),
 BOOKING_CANCELLATION_HOURS("INTEGER","Délai d'annulation des réservations",true),
 SUPPORT_SLA_HOURS("INTEGER","Objectif SLA support",false);
 public final String type;public final String description;public final boolean critical;
 SettingDefinition(String type,String description,boolean critical){this.type=type;this.description=description;this.critical=critical;}
 public static SettingDefinition require(String key){try{return valueOf(key.toUpperCase(Locale.ROOT));}catch(Exception e){throw new IllegalArgumentException("SETTING_NOT_ADMINISTRABLE");}}
 public void validate(String value){if(value==null||value.isBlank())throw new IllegalArgumentException("Setting value is required");switch(type){case"BOOLEAN"->{if(!Set.of("true","false").contains(value.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Boolean value expected");}case"INTEGER"->{int parsed;try{parsed=Integer.parseInt(value);}catch(Exception e){throw new IllegalArgumentException("Integer value expected");}if(parsed<0||parsed>100000)throw new IllegalArgumentException("Integer value out of range");}default->{if(value.length()>500)throw new IllegalArgumentException("Setting value too long");}}String lower=value.toLowerCase(Locale.ROOT);if(lower.matches(".*(secret|password|token|private.?key|api.?key|credential).*"))throw new IllegalArgumentException("SECRET_VALUE_REJECTED");}
}
