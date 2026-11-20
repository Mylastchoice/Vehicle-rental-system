package com.morrow.vehiclerental.repository;

import com.morrow.vehiclerental.model.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Page<Vehicle> findByActiveTrue(Pageable pageable);
    Page<Vehicle> findByActiveTrueAndCategory_Id(Long categoryId, Pageable pageable);

    boolean existsByVehicleCode(String vehicleCode);
    boolean existsByVehicleCodeAndIdNot(String vehicleCode, Long id);

    @Query("select v from Vehicle v where v.active = true and " +
           "(lower(v.name) like lower(concat('%', :keyword, '%')) or " +
           "lower(v.vehicleCode) like lower(concat('%', :keyword, '%')))")
    Page<Vehicle> search(@Param("keyword") String keyword, Pageable pageable);
}
