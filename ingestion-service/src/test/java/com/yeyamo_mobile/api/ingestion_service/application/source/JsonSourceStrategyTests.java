package com.yeyamo_mobile.api.ingestion_service.application.source;
import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;import com.fasterxml.jackson.databind.ObjectMapper;
class JsonSourceStrategyTests{
 @Test void readsArrayAndDataEnvelope(){JsonSourceStrategy s=new JsonSourceStrategy(new ObjectMapper());
  assertEquals(1,s.parse("[{\"name\":\"Kribi\"}]").size());assertEquals(1,s.parse("{\"data\":[{\"name\":\"Limbé\"}]}").size());}
}
