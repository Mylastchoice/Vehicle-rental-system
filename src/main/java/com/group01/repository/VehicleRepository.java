package com.group01.repository;

import com.group01.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Primary key is the vehicle code (String). Filters are built with {@code Specification} in CatalogService. */
public interface VehicleRepository extends JpaRepository<Vehicle, String>, JpaSpecificationExecutor<Vehicle> {
}
