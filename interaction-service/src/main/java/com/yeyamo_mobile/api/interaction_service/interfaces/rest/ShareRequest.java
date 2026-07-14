package com.yeyamo_mobile.api.interaction_service.interfaces.rest;
import jakarta.validation.constraints.Size;
public record ShareRequest(@Size(max=40)String channel){}
