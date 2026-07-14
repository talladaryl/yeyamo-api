package com.yeyamo_mobile.api.discovery_service.infrastructure.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.discovery_service.application.DiscoveryPage;
import com.yeyamo_mobile.api.discovery_service.application.DiscoverySearch;
import com.yeyamo_mobile.api.discovery_service.application.port.DiscoveryCachePort;

@Component
public class RedisDiscoveryCacheAdapter implements DiscoveryCachePort {
    private static final Logger log = LoggerFactory.getLogger(RedisDiscoveryCacheAdapter.class);
    private static final String VERSION = "discovery:cache:version";
    private final StringRedisTemplate redis; private final ObjectMapper mapper; private final Duration ttl;
    public RedisDiscoveryCacheAdapter(StringRedisTemplate redis,ObjectMapper mapper,@Value("${discovery.cache.ttl-seconds:60}") long seconds) {
        this.redis=redis; this.mapper=mapper; this.ttl=Duration.ofSeconds(Math.max(1,seconds));
    }
    public Optional<DiscoveryPage> get(DiscoverySearch search) {
        try { String value=redis.opsForValue().get(key(search)); return value==null?Optional.empty():Optional.of(mapper.readValue(value,DiscoveryPage.class)); }
        catch(Exception e) { log.debug("Discovery cache read unavailable: {}",e.getMessage()); return Optional.empty(); }
    }
    public void put(DiscoverySearch search,DiscoveryPage page) {
        try { redis.opsForValue().set(key(search),mapper.writeValueAsString(page),ttl); }
        catch(Exception e) { log.debug("Discovery cache write unavailable: {}",e.getMessage()); }
    }
    public void invalidate() {
        try { redis.opsForValue().increment(VERSION); }
        catch(RuntimeException e) { log.debug("Discovery cache invalidation unavailable: {}",e.getMessage()); }
    }
    private String key(DiscoverySearch search) throws Exception {
        String version=redis.opsForValue().get(VERSION); if(version==null)version="0";
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(search.toString().getBytes(StandardCharsets.UTF_8));
        return "discovery:search:v"+version+":"+HexFormat.of().formatHex(hash);
    }
}
