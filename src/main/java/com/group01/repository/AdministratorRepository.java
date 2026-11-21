package com.group01.repository;

import com.group01.entity.Administrator;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministratorRepository extends JpaRepository<Administrator, Long> {

    Optional<Administrator> findByEmailIgnoreCase(String email);
}
