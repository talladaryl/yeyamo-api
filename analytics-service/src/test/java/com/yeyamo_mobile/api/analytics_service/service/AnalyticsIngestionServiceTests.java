package com.yeyamo_mobile.api.analytics_service.service;

import static org.mockito.Mockito.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.analytics_service.enums.AnalyticsEventStatus;
import com.yeyamo_mobile.api.analytics_service.models.AnalyticsEventLog;
import com.yeyamo_mobile.api.analytics_service.repository.*;

class AnalyticsIngestionServiceTests {
    @Test
    void ignoresAnAlreadySuccessfulEvent() {
        UUID id=UUID.randomUUID();
        AnalyticsEventLog existing=new AnalyticsEventLog();existing.setEventId(id);existing.setStatus(AnalyticsEventStatus.SUCCESS);
        AnalyticsEventLogRepository logs=mock(AnalyticsEventLogRepository.class);
        KpiHistoryRepository kpis=mock(KpiHistoryRepository.class);
        when(logs.findByEventId(id)).thenReturn(Optional.of(existing));
        AnalyticsIngestionService service=new AnalyticsIngestionService(new ObjectMapper(),logs,kpis,mockKafka(),"audit.events");

        service.process(envelope(id));

        verifyNoInteractions(kpis);
        verify(logs,never()).save(any());
    }

    @Test
    void storesTheProjectionAndSuccessMarkerWithTheEventIdentity() {
        UUID id=UUID.randomUUID();
        AnalyticsEventLogRepository logs=mock(AnalyticsEventLogRepository.class);
        KpiHistoryRepository kpis=mock(KpiHistoryRepository.class);
        when(logs.findByEventId(id)).thenReturn(Optional.empty());
        AnalyticsIngestionService service=new AnalyticsIngestionService(new ObjectMapper(),logs,kpis,mockKafka(),"audit.events");

        service.process(envelope(id));

        verify(kpis).save(argThat(kpi->id.equals(kpi.getId())));
        verify(logs).save(argThat(log->id.equals(log.getId())&&log.getStatus()==AnalyticsEventStatus.SUCCESS));
    }

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String,String> mockKafka(){return mock(KafkaTemplate.class);}
    private String envelope(UUID id){return "{\"eventId\":\""+id+"\",\"eventType\":\"content.published\",\"eventVersion\":1,\"producer\":\"content-service\",\"payload\":{\"id\":\""+UUID.randomUUID()+"\"}}";}
}
