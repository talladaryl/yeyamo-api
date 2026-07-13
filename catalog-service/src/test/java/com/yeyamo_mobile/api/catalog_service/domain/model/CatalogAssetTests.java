package com.yeyamo_mobile.api.catalog_service.domain.model;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class CatalogAssetTests {
    @Test void validatesCoordinates(){assertThrows(IllegalArgumentException.class,()->new GeoPoint(91,0));}
    @Test void enforcesPublishingWorkflow(){
        CatalogAsset a=CatalogAsset.create(AssetType.PLACE,null,"catalog",null,"Musée","musee",
                null,"culture","CM-CE","Yaoundé",null,null,new GeoPoint(3.87,11.52));
        assertThrows(IllegalStateException.class,()->a.changeStatus(AssetStatus.PUBLISHED));
        a.changeStatus(AssetStatus.IN_REVIEW);a.changeStatus(AssetStatus.PUBLISHED);
        assertEquals(AssetStatus.PUBLISHED,a.getStatus());
    }

    @Test void softDeleteIsIdempotentAndFinal(){
        CatalogAsset asset=CatalogAsset.create(AssetType.EXPERIENCE,null,"catalog",null,"Visit","visit",null,null,null,null,null,null,new GeoPoint(3,11));
        asset.delete();asset.delete();
        assertEquals(AssetStatus.DELETED,asset.getStatus());
        assertThrows(IllegalStateException.class,()->asset.changeStatus(AssetStatus.DRAFT));
    }
}
