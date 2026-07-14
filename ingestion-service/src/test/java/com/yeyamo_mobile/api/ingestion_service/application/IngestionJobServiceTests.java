package com.yeyamo_mobile.api.ingestion_service.application;
import static org.junit.jupiter.api.Assertions.*;import java.util.*;import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.ingestion_service.domain.model.*;import com.yeyamo_mobile.api.ingestion_service.domain.port.ImportJobRepository;
class IngestionJobServiceTests{
 @Test void sameIdempotencyKeyReturnsSameJob(){MemoryJobs repo=new MemoryJobs();IngestionJobService service=new IngestionJobService(repo);
  ImportJob first=service.submit("partner-file-42",SourceType.JSON,null,"[]");ImportJob second=service.submit("partner-file-42",SourceType.JSON,null,"[{\"name\":\"ignored\"}]");
  assertEquals(first.getId(),second.getId());assertEquals(1,repo.jobs.size());}
 static class MemoryJobs implements ImportJobRepository{
  final Map<UUID,ImportJob> jobs=new HashMap<>();public ImportJob save(ImportJob j){jobs.put(j.getId(),j);return j;}
  public Optional<ImportJob> findById(UUID id){return Optional.ofNullable(jobs.get(id));}
  public Optional<ImportJob> findByIdempotencyKey(String key){return jobs.values().stream().filter(j->j.getIdempotencyKey().equals(key)).findFirst();}
  public List<ImportJob> findPending(int limit){return List.of();}
 }
}
