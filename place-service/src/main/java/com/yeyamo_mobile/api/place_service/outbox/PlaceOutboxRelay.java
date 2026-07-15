package com.yeyamo_mobile.api.place_service.outbox;
import java.time.Instant;import java.util.concurrent.TimeUnit;import org.slf4j.*;import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.kafka.core.KafkaTemplate;import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.Transactional;
@Component @ConditionalOnProperty(name="yeyamo.outbox.enabled",havingValue="true",matchIfMissing=true)
public class PlaceOutboxRelay{
 private static final Logger log=LoggerFactory.getLogger(PlaceOutboxRelay.class);private final PlaceOutboxRepository repository;private final KafkaTemplate<String,String> kafka;private final String topic;
 public PlaceOutboxRelay(PlaceOutboxRepository r,KafkaTemplate<String,String> k,@Value("${yeyamo.kafka.topics.place-events:place.events}")String t){repository=r;kafka=k;topic=t;}
 @Scheduled(fixedDelayString="${yeyamo.outbox.publish-delay-ms:1000}") @Transactional public void publishPending(){for(PlaceOutboxMessage m:repository.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc()){try{kafka.send(topic,m.getAggregateId(),m.getPayload()).get(5,TimeUnit.SECONDS);m.setPublishedAt(Instant.now());m.setLastError(null);}catch(Exception e){m.setAttempts(m.getAttempts()+1);String x=e.getMessage()==null?"Kafka publication failed":e.getMessage();m.setLastError(x.substring(0,Math.min(1000,x.length())));log.warn("Place outbox message {} publication failed",m.getId());}repository.save(m);}}
}
