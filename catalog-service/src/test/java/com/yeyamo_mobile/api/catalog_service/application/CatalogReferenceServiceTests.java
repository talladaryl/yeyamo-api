package com.yeyamo_mobile.api.catalog_service.application;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.catalog_service.domain.model.*;
import com.yeyamo_mobile.api.catalog_service.domain.port.CatalogReferenceRepository;

class CatalogReferenceServiceTests {
    @Test void requiresAnExistingActiveRegionForCities(){
        MemoryReferences repository=new MemoryReferences();CatalogReferenceService service=new CatalogReferenceService(repository,(a,b,c,d)->{});
        assertThrows(CatalogException.class,()->service.create(ReferenceType.CITY,"YDE","Yaoundé","CM-CE","CM",null,null,"admin"));
        service.create(ReferenceType.REGION,"CM-CE","Centre",null,"CM",null,null,"admin");
        CatalogReference city=service.create(ReferenceType.CITY,"YDE","Yaoundé","CM-CE","CM",null,null,"admin");
        assertEquals("CM-CE",city.getParentCode());
    }
    @Test void rejectsDuplicateCodesWithinTheSameType(){
        MemoryReferences repository=new MemoryReferences();CatalogReferenceService service=new CatalogReferenceService(repository,(a,b,c,d)->{});
        service.create(ReferenceType.CATEGORY,"CULTURE","Culture",null,null,null,null,"admin");
        CatalogException error=assertThrows(CatalogException.class,()->service.create(ReferenceType.CATEGORY,"culture","Other",null,null,null,null,"admin"));
        assertEquals("CATALOG_REFERENCE_EXISTS",error.getCode());
    }
    static final class MemoryReferences implements CatalogReferenceRepository{
        final Map<UUID,CatalogReference> values=new HashMap<>();
        public CatalogReference save(CatalogReference v){values.put(v.getId(),v);return v;}
        public Optional<CatalogReference> findById(UUID id){return Optional.ofNullable(values.get(id));}
        public Optional<CatalogReference> findByTypeAndCode(ReferenceType t,String c){return values.values().stream().filter(v->v.getType()==t&&v.getCode().equals(c)).findFirst();}
        public boolean existsByTypeAndCode(ReferenceType t,String c){return findByTypeAndCode(t,c).isPresent();}
        public List<CatalogReference> find(ReferenceType t,String p,boolean active,int limit){return values.values().stream().filter(v->v.getType()==t).filter(v->p==null||p.equals(v.getParentCode())).filter(v->!active||v.isActive()).limit(limit).toList();}
    }
}
