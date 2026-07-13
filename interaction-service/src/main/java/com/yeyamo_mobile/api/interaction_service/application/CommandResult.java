package com.yeyamo_mobile.api.interaction_service.application;
import java.util.UUID;
public record CommandResult(UUID resourceId,boolean changed,boolean replayed){}
