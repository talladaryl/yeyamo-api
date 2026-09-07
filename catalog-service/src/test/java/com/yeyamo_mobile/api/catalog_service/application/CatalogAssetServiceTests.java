package com.yeyamo_mobile.api.catalog_service.application;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.catalog_service.application.port.CatalogOutboxPort;
import com.yeyamo_mobile.api.catalog_service.domain.model.*;
import com.yeyamo_mobile.api.catalog_service.domain.port.CatalogAssetRepository;
import com.yeyamo_mobile.api.catalog_service.interfaces.rest.CatalogAssetResponse;
import com.yeyamo_mobile.shared.country.CountryConfigClient;
import static org.mockito.Mockito.mock;
class CatalogAssetServiceTests {
    @Test void createsCanonicalAssetAndEvent(){
        MemoryRepository repo=new MemoryRepository();List<String> events=new ArrayList<>();
        CatalogOutboxPort outbox=(type,asset,corr,actor)->events.add(type);
        CatalogAssetService service=new CatalogAssetService(repo,outbox,mock(CountryConfigClient.class));
        CatalogAsset asset=service.create(AssetType.PLACE,null,"Musée National",null,"Description",
                "culture","CM","CM-CE","Yaoundé",null,null,3.87,11.52,"corr","admin");
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
    @Test void returnsAllExperienceFieldsAndKeepsLegacyAssetsDeserializable(){
        MemoryRepository repo=new MemoryRepository(); CatalogAssetService service=new CatalogAssetService(repo,(a,b,c,d)->{});
        UUID placeId=UUID.randomUUID();
        service.synchronizeLegacyPlace(placeId.toString(),null,"Lieu","lieu",null,null,null,null,null,null,
                3,11,AssetStatus.PUBLISHED,"c");
        UUID mediaA=UUID.randomUUID(), mediaB=UUID.randomUUID();
        CatalogAsset asset=service.create(AssetType.EXPERIENCE,null,"Visite","visite-guidee","Description",
                "culture",null,"CM-CE","Yaounde",null,"Rue 1",3.87,11.52,List.of(mediaA,mediaB),90,"INTERMEDIATE",
                new BigDecimal("15000.00"),"XAF",2,10,List.of("Guide","Entree"),List.of("Transport"),placeId,"corr","partner");
        CatalogAssetResponse response=CatalogAssetResponse.from(asset);
        assertEquals(List.of(mediaA,mediaB),response.mediaIds()); assertEquals(90,response.durationMinutes());
        assertEquals("INTERMEDIATE",response.difficultyLevel()); assertEquals(new BigDecimal("15000.00"),response.price());
        assertEquals("XAF",response.currency()); assertEquals(2,response.capacityMin()); assertEquals(10,response.capacityMax());
        assertEquals(List.of("Guide","Entree"),response.includedItems()); assertEquals(List.of("Transport"),response.excludedItems());
        assertEquals(placeId,response.placeId());
        CatalogAsset legacy=CatalogAsset.create(AssetType.EXPERIENCE,null,"catalog",null,"Ancien","ancien",null,null,null,null,null,null,null,new GeoPoint(3,11));
        CatalogAssetResponse legacyResponse=CatalogAssetResponse.from(legacy);
        assertEquals(List.of(),legacyResponse.mediaIds()); assertNull(legacyResponse.durationMinutes()); assertNull(legacyResponse.placeId());
    }
    @Test void rejectsUnknownOrInactiveLinkedPlace(){
        MemoryRepository repo=new MemoryRepository(); CatalogAssetService service=new CatalogAssetService(repo,(a,b,c,d)->{});
        CatalogException unknown=assertThrows(CatalogException.class,()->service.create(AssetType.EXPERIENCE,null,"Exp","exp",null,
                null,null,null,null,null,null,3,11,null,null,null,null,null,null,null,null,null,UUID.randomUUID(),"corr","partner"));
        assertEquals("PLACE_NOT_AVAILABLE",unknown.getCode());
        UUID inactivePlace=UUID.randomUUID(); service.synchronizeLegacyPlace(inactivePlace.toString(),null,"Lieu","lieu",null,null,null,null,null,null,3,11,AssetStatus.DRAFT,"c");
        CatalogException inactive=assertThrows(CatalogException.class,()->service.create(AssetType.EXPERIENCE,null,"Exp 2","exp-2",null,
                null,null,null,null,null,null,3,11,null,null,null,null,null,null,null,null,null,inactivePlace,"corr","partner"));
        assertEquals("PLACE_NOT_AVAILABLE",inactive.getCode());
    }
    static class MemoryRepository implements CatalogAssetRepository {
        final Map<UUID,CatalogAsset> data=new HashMap<>();
        public CatalogAsset save(CatalogAsset a){data.put(a.getId(),a);return a;}
        public Optional<CatalogAsset> findById(UUID id){return Optional.ofNullable(data.get(id));}
        public boolean existsById(UUID id){return data.containsKey(id);}
        public List<CatalogAsset> findAllById(List<UUID> ids){return ids.stream().map(data::get).filter(Objects::nonNull).toList();}
        public Optional<CatalogAsset> findBySlug(String s){return data.values().stream().filter(a->a.getSlug().equals(s)).findFirst();}
        public Optional<CatalogAsset> findBySourceAndExternalId(String s,String e){return data.values().stream().filter(a->s.equals(a.getSource())&&e.equals(a.getExternalId())).findFirst();}
        public boolean existsBySlugAndIdNot(String s,UUID id){return data.values().stream().anyMatch(a->a.getSlug().equals(s)&&!a.getId().equals(id));}
        public List<CatalogAsset> search(AssetStatus s,AssetType t,String r,String c,String q,int l){return List.copyOf(data.values());}
        public List<CatalogAsset> findNearby(double a,double b,double c,AssetType d,String e,int f){return List.copyOf(data.values());}
    }
}
