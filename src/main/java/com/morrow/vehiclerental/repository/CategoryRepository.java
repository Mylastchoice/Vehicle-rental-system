package com.morrow.vehiclerental.repository;

import com.morrow.vehiclerental.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {}
