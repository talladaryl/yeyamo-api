package com.yeyamo_mobile.api.ingestion_service.application.source;
import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.ingestion_service.domain.model.*;
class CsvSourceStrategyTests{
 @Test void parsesQuotedCsv(){ImportJob j=ImportJob.create("key",SourceType.CSV,"partner-a","name,description,latitude,longitude\n\"Musée, National\",\"Art, culture\",3.87,11.52");
  var rows=new CsvSourceStrategy().extract(j);assertEquals(1,rows.size());assertEquals("Musée, National",rows.getFirst().values().get("name"));assertEquals("Art, culture",rows.getFirst().values().get("description"));}
}
