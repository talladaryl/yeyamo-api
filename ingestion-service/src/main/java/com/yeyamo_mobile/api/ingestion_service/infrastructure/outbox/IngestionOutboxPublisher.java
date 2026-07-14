package com.yeyamo_mobile.api.ingestion_service.infrastructure.outbox;
import java.time.Instant;import java.util.concurrent.TimeUnit;import org.slf4j.*;import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.kafka.core.KafkaTemplate;import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.Transactional;
@Component @ConditionalOnProperty(name="ingestion.outbox.enabled",havingValue="true",matchIfMissing=true)
public class IngestionOutboxPublisher{
 private static final Logger log=LoggerFactory.getLogger(IngestionOutboxPublisher.class);private final IngestionOutboxRepository repo;private final KafkaTemplate<String,String> kafka;private final String topic;
 public IngestionOutboxPublisher(IngestionOutboxRepository r,KafkaTemplate<String,String> k,@Value("${yeyamo.kafka.topics.catalog-ingestion-events:catalog.ingestion.events}")String t){repo=r;kafka=k;topic=t;}
 @Scheduled(fixedDelayString="${ingestion.outbox.delay-ms:1000}")@Transactional public void publish(){
  for(IngestionOutboxEvent e:repo.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc()){try{kafka.send(topic,e.getAggregateId(),e.getPayload()).get(5,TimeUnit.SECONDS);e.setPublishedAt(Instant.now());e.setLastError(null);}
   catch(Exception ex){e.setAttempts(e.getAttempts()+1);String m=ex.getMessage()==null?"Kafka failure":ex.getMessage();e.setLastError(m.substring(0,Math.min(m.length(),1000)));log.warn("Outbox event {} failed",e.getId());}repo.save(e);}
 }
}
