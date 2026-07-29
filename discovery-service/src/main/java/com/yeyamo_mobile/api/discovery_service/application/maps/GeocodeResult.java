package com.yeyamo_mobile.api.discovery_service.application.maps;
import java.util.Map;public record GeocodeResult(String formattedAddress,double latitude,double longitude,String placeId,Map<String,String>components){}
