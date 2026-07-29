package com.yeyamo_mobile.api.notification_service.infrastructure.channel;
import org.springframework.context.annotation.*;import org.springframework.web.client.RestClient;
@Configuration public class PushHttpClientConfig{@Bean RestClient.Builder restClientBuilder(){return RestClient.builder();}}
