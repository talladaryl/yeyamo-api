package com.yeyamo_mobile.api.catalog_service.application;
import java.time.Instant;import org.springframework.data.domain.*;import org.springframework.data.jpa.domain.Specification;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import com.yeyamo_mobile.api.catalog_service.domain.model.*;import com.yeyamo_mobile.api.catalog_service.infrastructure.persistence.*;import com.yeyamo_mobile.api.catalog_service.interfaces.rest.AdminCatalogAssetResponse;
@Service public class AdminCatalogAssetService{
 private final SpringDataCatalogAssetRepository repository;public AdminCatalogAssetService(SpringDataCatalogAssetRepository repository){this.repository=repository;}
 @Transactional(readOnly=true)public Page<AdminCatalogAssetResponse>search(String search,AssetType type,AssetStatus status,String regionCode,String categoryCode,String source,Instant createdFrom,Instant createdTo,Pageable pageable){
  Specification<CatalogAssetEntity>s=(r,q,c)->c.conjunction();
  if(search!=null&&!search.isBlank())s=s.and((r,q,c)->c.or(c.like(c.lower(r.get("name")),"%"+search.trim().toLowerCase()+"%"),c.like(c.lower(r.get("description")),"%"+search.trim().toLowerCase()+"%")));
  if(type!=null)s=s.and((r,q,c)->c.equal(r.get("type"),type));if(status!=null)s=s.and((r,q,c)->c.equal(r.get("status"),status));
  if(regionCode!=null&&!regionCode.isBlank())s=s.and((r,q,c)->c.equal(r.get("regionCode"),regionCode));if(categoryCode!=null&&!categoryCode.isBlank())s=s.and((r,q,c)->c.equal(r.get("categoryCode"),categoryCode));
  if(source!=null&&!source.isBlank())s=s.and((r,q,c)->c.equal(r.get("source"),source));if(createdFrom!=null)s=s.and((r,q,c)->c.greaterThanOrEqualTo(r.get("createdAt"),createdFrom));if(createdTo!=null)s=s.and((r,q,c)->c.lessThanOrEqualTo(r.get("createdAt"),createdTo));
  return repository.findAll(s,pageable).map(AdminCatalogAssetResponse::from);
 }
}
