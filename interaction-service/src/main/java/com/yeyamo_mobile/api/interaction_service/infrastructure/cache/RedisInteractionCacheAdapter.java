package com.yeyamo_mobile.api.interaction_service.infrastructure.cache;
import java.time.Duration;import java.util.*;import org.slf4j.*;import org.springframework.beans.factory.annotation.Value;import org.springframework.data.redis.core.StringRedisTemplate;import org.springframework.stereotype.Component;
import com.yeyamo_mobile.api.interaction_service.application.InteractionSummary;import com.yeyamo_mobile.api.interaction_service.application.port.InteractionCachePort;
@Component
public class RedisInteractionCacheAdapter implements InteractionCachePort{
 private static final Logger log=LoggerFactory.getLogger(RedisInteractionCacheAdapter.class);private final StringRedisTemplate redis;private final Duration ttl;
 public RedisInteractionCacheAdapter(StringRedisTemplate redis,@Value("${interaction.cache.ttl-seconds:60}")long seconds){this.redis=redis;ttl=Duration.ofSeconds(seconds);}
 public Optional<InteractionSummary.Counts>getCounts(UUID postId){try{String value=redis.opsForValue().get(key(postId));if(value==null)return Optional.empty();String[] p=value.split("\\|");
  if(p.length!=3)return Optional.empty();return Optional.of(new InteractionSummary.Counts(Long.parseLong(p[0]),Long.parseLong(p[1]),Long.parseLong(p[2])));
 }catch(RuntimeException e){log.debug("Redis read unavailable: {}",e.getMessage());return Optional.empty();}}
 public void putCounts(UUID postId,InteractionSummary.Counts c){try{redis.opsForValue().set(key(postId),c.likes()+"|"+c.comments()+"|"+c.shares(),ttl);}catch(RuntimeException e){log.debug("Redis write unavailable: {}",e.getMessage());}}
 public void evict(UUID postId){try{redis.delete(key(postId));}catch(RuntimeException e){log.debug("Redis eviction unavailable: {}",e.getMessage());}}
 private String key(UUID id){return "interaction:post:"+id+":counts";}
}
