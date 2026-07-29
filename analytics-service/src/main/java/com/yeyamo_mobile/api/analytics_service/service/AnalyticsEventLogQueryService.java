package com.yeyamo_mobile.api.analytics_service.service;
import java.time.*;import org.springframework.data.domain.*;import org.springframework.data.elasticsearch.core.*;import org.springframework.data.elasticsearch.core.query.*;import org.springframework.stereotype.Service;import com.yeyamo_mobile.api.analytics_service.models.AnalyticsEventLog;
@Service public class AnalyticsEventLogQueryService{
 private final ElasticsearchOperations elasticsearch;public AnalyticsEventLogQueryService(ElasticsearchOperations elasticsearch){this.elasticsearch=elasticsearch;}
 public Page<AnalyticsEventLog> search(String eventType,String userId,String service,String correlationId,Instant from,Instant to,Pageable pageable){
  Criteria criteria=new Criteria();
  if(has(eventType))criteria=criteria.and(new Criteria("eventType").is(eventType));
  if(has(userId))criteria=criteria.and(new Criteria("userId").is(userId));
  if(has(service))criteria=criteria.and(new Criteria("service").is(service));
  if(has(correlationId))criteria=criteria.and(new Criteria("correlationId").is(correlationId));
  if(from!=null)criteria=criteria.and(new Criteria("occurredAt").greaterThanEqual(from));
  if(to!=null)criteria=criteria.and(new Criteria("occurredAt").lessThanEqual(to));
  CriteriaQuery query=new CriteriaQuery(criteria).setPageable(pageable);
  SearchHits<AnalyticsEventLog> hits=elasticsearch.search(query,AnalyticsEventLog.class);
  return new PageImpl<>(hits.stream().map(SearchHit::getContent).toList(),pageable,hits.getTotalHits());
 }
 private boolean has(String value){return value!=null&&!value.isBlank();}
}
