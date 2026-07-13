package com.yeyamo_mobile.api.interaction_service.interfaces.rest;
import java.util.UUID;import com.yeyamo_mobile.api.interaction_service.application.CommandResult;
public record CommandResponse(UUID resourceId,boolean changed,boolean replayed){public static CommandResponse from(CommandResult r){return new CommandResponse(r.resourceId(),r.changed(),r.replayed());}}
