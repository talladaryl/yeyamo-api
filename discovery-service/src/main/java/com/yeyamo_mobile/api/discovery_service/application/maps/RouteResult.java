package com.yeyamo_mobile.api.discovery_service.application.maps;
public record RouteResult(long distanceMeters,long durationSeconds,String encodedPolyline,Coordinate origin,Coordinate destination){}
