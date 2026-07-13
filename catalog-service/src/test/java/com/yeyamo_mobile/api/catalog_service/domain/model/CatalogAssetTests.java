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
}
