package com.yeyamo_mobile.api.notification_service.application;
import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Component;
@Component public class DeliveryScheduler{private final DeliveryService service;public DeliveryScheduler(DeliveryService s){service=s;}@Scheduled(fixedDelayString="${notification.delivery.poll-delay-ms:5000}")public void run(){service.dispatch(100);}}
