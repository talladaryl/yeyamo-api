package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.domain.PageRequest;import org.springframework.stereotype.Component;import com.yeyamo_mobile.api.interaction_service.domain.model.CheckIn;import com.yeyamo_mobile.api.interaction_service.domain.port.CheckInRepository;
@Component
public class JpaCheckInRepositoryAdapter implements CheckInRepository{
 private final SpringCheckInRepository repo;public JpaCheckInRepositoryAdapter(SpringCheckInRepository r){repo=r;}public CheckIn save(CheckIn c){CheckInEntity e=new CheckInEntity();e.setId(c.id());e.setCatalogAssetId(c.catalogAssetId());e.setUserId(c.userId());e.setLatitude(c.latitude());e.setLongitude(c.longitude());e.setVisible(c.visible());e.setOccurredAt(c.occurredAt());return domain(repo.save(e));}
 public Optional<CheckIn>findById(UUID id){return repo.findById(id).map(this::domain);}public List<CheckIn>findByUser(String u,int l){return repo.findByUserIdOrderByOccurredAtDesc(u,PageRequest.of(0,l)).stream().map(this::domain).toList();}
 private CheckIn domain(CheckInEntity e){return new CheckIn(e.getId(),e.getCatalogAssetId(),e.getUserId(),e.getLatitude(),e.getLongitude(),e.isVisible(),e.getOccurredAt());}}
