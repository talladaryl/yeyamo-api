package com.yeyamo_mobile.api.discovery_service.application.port;
import com.yeyamo_mobile.api.discovery_service.application.maps.*;public interface ExternalMapsPort{GeocodeResult geocode(String address);GeocodeResult reverseGeocode(Coordinate coordinate);RouteResult route(Coordinate origin,Coordinate destination,TravelMode mode);}
