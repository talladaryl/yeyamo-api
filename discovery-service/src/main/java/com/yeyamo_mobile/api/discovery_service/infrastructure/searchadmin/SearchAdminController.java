package com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin;
import static com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin.SearchAdminDtos.*;
import java.util.*;import jakarta.validation.Valid;import org.springframework.data.domain.*;import org.springframework.data.web.PageableDefault;import org.springframework.http.HttpStatus;import org.springframework.security.oauth2.jwt.Jwt;import org.springframework.web.bind.annotation.*;
@RestController@RequestMapping("/api/v1/admin/search")public class SearchAdminController {
 private final SearchAdminService service;public SearchAdminController(SearchAdminService s){service=s;}
 @GetMapping("/overview")public Overview overview(){return service.overview();}@GetMapping("/indexes")public List<IndexInfo> indexes(){return service.indexes();}
 @PostMapping("/reindex")@ResponseStatus(HttpStatus.ACCEPTED)public ReindexResponse reindex(Jwt jwt){return service.reindex(jwt.getSubject());}
 @GetMapping("/synonyms")public List<SynonymResponse> synonyms(){return service.synonyms();}@PostMapping("/synonyms")@ResponseStatus(HttpStatus.CREATED)public SynonymResponse create(@Valid@RequestBody SynonymRequest r,Jwt jwt){return service.createSynonym(jwt.getSubject(),r);}
 @PutMapping("/synonyms/{id}")public SynonymResponse update(@PathVariable UUID id,@Valid@RequestBody SynonymRequest r){return service.updateSynonym(id,r);}@DeleteMapping("/synonyms/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID id){service.deleteSynonym(id);}
 @GetMapping("/ranking")public RankingResponse ranking(){return service.ranking();}@PutMapping("/ranking")public RankingResponse ranking(@Valid@RequestBody RankingRequest r,Jwt jwt){return service.ranking(jwt.getSubject(),r);}
 @GetMapping("/zero-results")public Page<ZeroResult> zero(@PageableDefault(size=25)Pageable p){return service.zeroResults(p);}
}
