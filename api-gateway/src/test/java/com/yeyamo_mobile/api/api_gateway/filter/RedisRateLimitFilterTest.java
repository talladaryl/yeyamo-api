package com.yeyamo_mobile.api.api_gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RedisRateLimitFilterTest {

    @Test
    void usesCategorySpecificLimitForArtworkOrderCreation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/artwork-orders");
        MockHttpServletResponse response = apply(request);

        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("30");
    }

    @Test
    void usesAudioLimitForCultureAudioUploads() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/media/culture");
        request.addParameter("usageType", "CULTURE_STORY_AUDIO");
        MockHttpServletResponse response = apply(request);

        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
    }

    private MockHttpServletResponse apply(MockHttpServletRequest request) throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);
        RedisRateLimitFilter filter = new RedisRateLimitFilter(redis, true, false,
                120, 20, 10, 20, 30, 50, 20, 30, 10, 60);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });
        return response;
    }
}
