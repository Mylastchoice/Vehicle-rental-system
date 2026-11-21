package com.group01.repository;

import com.group01.entity.RentalCart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalCartRepository extends JpaRepository<RentalCart, Long> {

    Optional<RentalCart> findByUser_UserId(Long userId);
}
