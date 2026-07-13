package com.yeyamo_mobile.api.catalog_service.application;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.catalog_service.application.port.CatalogOutboxPort;
import com.yeyamo_mobile.api.catalog_service.domain.model.*;
import com.yeyamo_mobile.api.catalog_service.domain.port.CatalogAssetRepository;
class CatalogAssetServiceTests {
    @Test void createsCanonicalAssetAndEvent(){
        MemoryRepository repo=new MemoryRepository();List<String> events=new ArrayList<>();
        CatalogOutboxPort outbox=(type,asset,corr,actor)->events.add(type);
        CatalogAssetService service=new CatalogAssetService(repo,outbox);
        CatalogAsset asset=service.create(AssetType.PLACE,null,"Musée National",null,"Description",
                "culture","CM-CE","Yaoundé",null,null,3.87,11.52,"corr","admin");
        assertEquals("musee-national",asset.getSlug());assertEquals(AssetStatus.DRAFT,asset.getStatus());
        assertEquals(List.of("catalog.asset.created"),events);
    }
    @Test void synchronizesLegacyPlaceIdempotently(){
        MemoryRepository repo=new MemoryRepository();CatalogAssetService service=new CatalogAssetService(repo,(a,b,c,d)->{});
        service.synchronizeLegacyPlace(UUID.randomUUID().toString(),null,"Lieu","lieu",null,null,null,null,null,null,
                3,11,AssetStatus.PUBLISHED,"c");
        String external=repo.data.values().iterator().next().getExternalId();
        service.synchronizeLegacyPlace(external,null,"Lieu modifié","lieu",null,null,null,null,null,null,
                3,11,AssetStatus.PUBLISHED,"c");
        assertEquals(1,repo.data.size());assertEquals("Lieu modifié",repo.data.values().iterator().next().getName());
    }
    static class MemoryRepository implements CatalogAssetRepository {
        final Map<UUID,CatalogAsset> data=new HashMap<>();
        public CatalogAsset save(CatalogAsset a){data.put(a.getId(),a);return a;}
        public Optional<CatalogAsset> findById(UUID id){return Optional.ofNullable(data.get(id));}
        public Optional<CatalogAsset> findBySlug(String s){return data.values().stream().filter(a->a.getSlug().equals(s)).findFirst();}
        public Optional<CatalogAsset> findBySourceAndExternalId(String s,String e){return data.values().stream().filter(a->s.equals(a.getSource())&&e.equals(a.getExternalId())).findFirst();}
        public boolean existsBySlugAndIdNot(String s,UUID id){return data.values().stream().anyMatch(a->a.getSlug().equals(s)&&!a.getId().equals(id));}
        public List<CatalogAsset> search(AssetStatus s,AssetType t,String r,String c,String q,int l){return List.copyOf(data.values());}
        public List<CatalogAsset> findNearby(double a,double b,double c,AssetType d,String e,int f){return List.copyOf(data.values());}
    }
}
