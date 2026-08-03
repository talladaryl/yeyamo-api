package com.yeyamo_mobile.api.place_service.repository;

import com.yeyamo_mobile.api.place_service.models.Country;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, String> {
    List<Country> findAllByActiveTrueOrderByNameAsc();
}
