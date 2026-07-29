package com.yeyamo_mobile.api.discovery_service.application.maps;
public record Coordinate(double latitude,double longitude){public Coordinate{if(latitude< -90||latitude>90||longitude< -180||longitude>180)throw new IllegalArgumentException("Invalid coordinates");}}
