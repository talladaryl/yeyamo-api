package com.yeyamo_mobile.api.country_config_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.yeyamo_mobile.api.country_config_service.domain.model.CountryLaunchStatus;
import com.yeyamo_mobile.api.country_config_service.dto.CountryDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

class CacheConfigTest {

    private final RedisSerializer<Object> serializer = CacheConfig.redisValueSerializer();

    @Test
    void serializesAndDeserializesCountryDtoWithInstant() {
        CountryDto country = countryDto();

        Object restored = serializer.deserialize(serializer.serialize(country));

        assertThat(restored).isInstanceOf(CountryDto.class);
        assertThat((CountryDto) restored).isEqualTo(country);
    }

    @Test
    void serializesAndDeserializesCachedCountryListWithInstant() {
        List<CountryDto> countries = List.of(countryDto());

        Object restored = serializer.deserialize(serializer.serialize(countries));

        assertThat(restored).isInstanceOf(List.class);
        List<?> restoredCountries = (List<?>) restored;
        assertThat(restoredCountries).hasSize(1);
        assertThat(restoredCountries.getFirst()).isEqualTo(countries.getFirst());
    }

    private CountryDto countryDto() {
        Instant createdAt = Instant.parse("2026-09-13T12:00:00Z");
        return new CountryDto(
                UUID.fromString("e0186c42-b09f-4d91-a6fd-d5e64f50f15e"),
                "CM", "Cameroon", "Republic of Cameroon", "AF",
                "fr", "XAF", "Africa/Douala", "+237", CountryLaunchStatus.LIVE,
                true, true, true, true, true, true, true, true, true, true,
                createdAt, createdAt.plusSeconds(60));
    }
}
