package com.yeyamo_mobile.api.ingestion_service.domain.model;
import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;
class ImportJobTests{
 @Test void followsLifecycleAndClearsPayload(){ImportJob j=ImportJob.create("same",SourceType.JSON,null,"[]");j.start();j.complete(2,1,0,1);
  assertEquals(JobStatus.COMPLETED,j.getStatus());assertNull(j.getInputPayload());assertEquals(1,j.getDuplicateRecords());}
 @Test void requiresPayloadForFileSources(){assertThrows(IllegalArgumentException.class,()->ImportJob.create("k",SourceType.CSV,null,null));}
}
