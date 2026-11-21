package com.group01.repository;

import com.group01.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Primary key is the vehicle code (String). Filters are built with {@code Specification} in CatalogService. */
public interface VehicleRepository extends JpaRepository<Vehicle, String>, JpaSpecificationExecutor<Vehicle> {
    @Modifying
    @Query("update Vehicle v set v.viewCount = v.viewCount + 1 where v.vehicleCode = :code")
    int incrementViewCount(@Param("code") String vehicleCode);
}
