package com.yeyamo_mobile.api.place_service.event;

import java.time.Instant;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.place_service.models.PartnerReadModel;
import com.yeyamo_mobile.api.place_service.repository.PartnerReadModelRepository;

@Component
public class PartnerEventConsumer {
    private final ObjectMapper mapper; private final PartnerReadModelRepository partners;
    public PartnerEventConsumer(ObjectMapper mapper, PartnerReadModelRepository partners) { this.mapper=mapper; this.partners=partners; }
    @KafkaListener(topics="${yeyamo.kafka.topics.partner-events:partner-events}", groupId="${spring.kafka.consumer.group-id:place-service}")
    @Transactional public void consume(String raw) throws Exception {
        JsonNode event=mapper.readTree(raw); String type=text(event,"eventType"); if(type==null||!type.startsWith("partner.")) return;
        JsonNode payload=event.path("payload"); String rawPartnerId=text(payload,"partnerId"); String owner=text(payload,"ownerUserId"); String status=text(payload,"status");
        if(rawPartnerId==null||owner==null||status==null) return;
        UUID partnerId=UUID.fromString(rawPartnerId); PartnerReadModel partner=partners.findById(partnerId).orElseGet(()->new PartnerReadModel(partnerId,owner,status,Instant.now()));
        partner.setOwnerUserId(owner); partner.setStatus(status); partner.setUpdatedAt(Instant.now()); partners.save(partner);
    }
    private String text(JsonNode node,String field){JsonNode value=node.get(field);return value==null||value.isNull()?null:value.asText();}
}
