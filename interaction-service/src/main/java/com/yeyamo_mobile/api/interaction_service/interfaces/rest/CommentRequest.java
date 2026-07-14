package com.yeyamo_mobile.api.interaction_service.interfaces.rest;
import java.util.UUID;import jakarta.validation.constraints.*;
public record CommentRequest(UUID parentId,@NotBlank@Size(max=2000)String body){}
