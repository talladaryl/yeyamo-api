package com.yeyamo_mobile.api.analytics_service.models;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import lombok.Getter;
import lombok.Setter;

@Document(indexName = "user_engagement")
@Getter
@Setter
public class UserEngagement {

    @Id
    private UUID id = UUID.randomUUID();

    private UUID userId;
    private LocalDate engagementDate;
    private Integer postsCount = 0;
    private Integer likesGiven = 0;
    private Integer likesReceived = 0;
    private Integer commentsCount = 0;
}
