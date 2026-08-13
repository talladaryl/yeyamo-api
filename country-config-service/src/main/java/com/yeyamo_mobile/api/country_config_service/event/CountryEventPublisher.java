package com.yeyamo_mobile.api.country_config_service.event;

import com.yeyamo_mobile.api.country_config_service.domain.model.Country;
import com.yeyamo_mobile.api.country_config_service.domain.model.CountryLaunchStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes country configuration events to Kafka.
 * Other microservices can subscribe to stay in sync.
 */
@Component
public class CountryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CountryEventPublisher.class);
    private static final String TOPIC = "country-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CountryEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCountryCreated(Country country) {
        var event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "CountryCreated",
                "timestamp", Instant.now().toString(),
                "countryCode", country.getCode(),
                "countryName", country.getName(),
                "launchStatus", country.getLaunchStatus().name()
        );

        kafkaTemplate.send(TOPIC, country.getCode(), event);
        log.info("Published CountryCreated event for country: {}", country.getCode());
    }

    public void publishCountryConfigurationUpdated(Country country) {
        var event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "CountryConfigurationUpdated",
                "timestamp", Instant.now().toString(),
                "countryCode", country.getCode(),
                "countryName", country.getName(),
                "defaultLanguageCode", country.getDefaultLanguageCode(),
                "defaultCurrencyCode", country.getDefaultCurrencyCode(),
                "defaultTimezone", country.getDefaultTimezone(),
                "phoneCountryCode", country.getPhoneCountryCode()
        );

        kafkaTemplate.send(TOPIC, country.getCode(), event);
        log.info("Published CountryConfigurationUpdated event for country: {}", country.getCode());
    }

    public void publishCountryLaunchStatusChanged(Country country, CountryLaunchStatus oldStatus, CountryLaunchStatus newStatus) {
        var event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "CountryLaunchStatusChanged",
                "timestamp", Instant.now().toString(),
                "countryCode", country.getCode(),
                "countryName", country.getName(),
                "oldStatus", oldStatus.name(),
                "newStatus", newStatus.name()
        );

        kafkaTemplate.send(TOPIC, country.getCode(), event);
        log.info("Published CountryLaunchStatusChanged event for country: {} ({} -> {})", 
                country.getCode(), oldStatus, newStatus);
    }

    public void publishCountryFeatureChanged(Country country) {
        var event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "CountryFeatureChanged",
                "timestamp", Instant.now().toString(),
                "countryCode", country.getCode(),
                "countryName", country.getName(),
                "features", Map.of(
                        "registrationEnabled", country.getRegistrationEnabled(),
                        "contentPublishingEnabled", country.getContentPublishingEnabled(),
                        "placePublishingEnabled", country.getPlacePublishingEnabled(),
                        "eventFeatureEnabled", country.getEventFeatureEnabled(),
                        "partnerOnboardingEnabled", country.getPartnerOnboardingEnabled(),
                        "paymentsEnabled", country.getPaymentsEnabled(),
                        "bookingEnabled", country.getBookingEnabled(),
                        "ticketingEnabled", country.getTicketingEnabled(),
                        "artisanCommerceEnabled", country.getArtisanCommerceEnabled(),
                        "cultureModuleEnabled", country.getCultureModuleEnabled()
                )
        );

        kafkaTemplate.send(TOPIC, country.getCode(), event);
        log.info("Published CountryFeatureChanged event for country: {}", country.getCode());
    }
}
