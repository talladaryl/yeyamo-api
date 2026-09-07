package com.yeyamo_mobile.api.catalog_service.interfaces.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yeyamo_mobile.api.catalog_service.application.CatalogException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTests {
    @Test
    void mapsUnavailablePlaceToTheStandardCatalogErrorBody() {
        var response = new GlobalExceptionHandler().catalog(
                new CatalogException("PLACE_NOT_AVAILABLE", "Referenced place does not exist or is not active"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("PLACE_NOT_AVAILABLE", response.getBody().get("code"));
        assertEquals("Referenced place does not exist or is not active", response.getBody().get("message"));
    }
}
