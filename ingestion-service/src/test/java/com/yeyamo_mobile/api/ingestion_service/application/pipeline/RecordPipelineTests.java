package com.yeyamo_mobile.api.ingestion_service.application.pipeline;
import static org.junit.jupiter.api.Assertions.*;import java.util.Map;import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.ingestion_service.application.source.RawRecord;
class RecordPipelineTests{
 @Test void normalizesAliasesAndValidates(){var raw=new RawRecord(1,Map.of("nom"," Musée National ","type","place","categorie","Arts & Culture","lat","3.87","lng","11.52"));
  var record=new RecordValidator().validate(new RecordNormalizer().normalize("partner-a",raw));
  assertTrue(record.valid());assertEquals("PLACE",record.assetType());assertEquals("arts-culture",record.categoryCode());assertEquals(64,record.fingerprint().length());}
 @Test void rejectsIncompleteRecord(){var raw=new RawRecord(1,Map.of("name","x","latitude","200","longitude","11"));
  assertFalse(new RecordValidator().validate(new RecordNormalizer().normalize("a",raw)).valid());}
}
