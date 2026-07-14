package com.yeyamo_mobile.api.interaction_service.infrastructure.outbox;
import java.time.Instant;import java.util.concurrent.TimeUnit;import org.slf4j.*;import org.springframework.beans.factory.annotation.Value;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.Transactional;
@Component @ConditionalOnProperty(name="interaction.outbox.enabled",havingValue="true",matchIfMissing=true)
public class InteractionOutboxPublisher{
 private static final Logger log=LoggerFactory.getLogger(InteractionOutboxPublisher.class);private final InteractionOutboxRepository repo;private final KafkaTemplate<String,String> kafka;private final String topic;
 public InteractionOutboxPublisher(InteractionOutboxRepository r,KafkaTemplate<String,String> k,@Value("${yeyamo.kafka.topics.interaction-events:interaction.events}")String t){repo=r;kafka=k;topic=t;}
 @Scheduled(fixedDelayString="${interaction.outbox.delay-ms:1000}")@Transactional public void publish(){for(InteractionOutboxEvent e:repo.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc()){try{kafka.send(topic,e.getAggregateId(),e.getPayload()).get(5,TimeUnit.SECONDS);e.setPublishedAt(Instant.now());e.setLastError(null);}
  catch(Exception x){e.setAttempts(e.getAttempts()+1);String m=x.getMessage()==null?"Kafka failure":x.getMessage();e.setLastError(m.substring(0,Math.min(m.length(),1000)));log.warn("Interaction outbox event {} failed",e.getId());}repo.save(e);}}
}
