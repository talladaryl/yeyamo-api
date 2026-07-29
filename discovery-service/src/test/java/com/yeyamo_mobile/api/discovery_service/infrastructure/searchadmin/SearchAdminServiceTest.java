package com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;import com.fasterxml.jackson.databind.ObjectMapper;import org.junit.jupiter.api.Test;import org.springframework.core.task.SyncTaskExecutor;
class SearchAdminServiceTest {
 @Test void rejectsConcurrentGlobalReindex(){var synonyms=mock(SearchSynonymRepository.class);var rankings=mock(SearchRankingRepository.class);var jobs=mock(SearchReindexJobRepository.class);var zero=mock(SearchZeroResultRepository.class);var client=mock(OpenSearchAdminClient.class);when(jobs.existsByStatusIn(anyCollection())).thenReturn(true);var service=new SearchAdminService(synonyms,rankings,jobs,zero,client,new ObjectMapper(),new SyncTaskExecutor());var error=assertThrows(IllegalStateException.class,()->service.reindex("admin"));assertEquals("REINDEX_ALREADY_RUNNING",error.getMessage());verifyNoInteractions(client);}
}
