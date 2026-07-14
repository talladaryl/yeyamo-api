package com.yeyamo_mobile.api.catalog_service.domain.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CatalogReferenceTests {
    @Test void normalizesAndValidatesGeographicReferences(){
        CatalogReference region=CatalogReference.create(ReferenceType.REGION,"cm-ce","Centre",null,"cm",null);
        CatalogReference city=CatalogReference.create(ReferenceType.CITY,"yde","Yaoundé","cm-ce","cm",null);
        assertEquals("CM-CE",region.getCode());assertEquals("CM",region.getCountryCode());
        assertEquals("CM-CE",city.getParentCode());
        assertThrows(IllegalArgumentException.class,()->CatalogReference.create(ReferenceType.CITY,"dla","Douala",null,"CM",null));
    }
    @Test void referencesUseSoftDeactivation(){
        CatalogReference category=CatalogReference.create(ReferenceType.CATEGORY,"culture","Culture",null,null,null);
        category.deactivate();assertFalse(category.isActive());category.activate();assertTrue(category.isActive());
    }
}
