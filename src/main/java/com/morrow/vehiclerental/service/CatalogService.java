package com.morrow.vehiclerental.service;

import com.morrow.vehiclerental.model.Vehicle;
import com.morrow.vehiclerental.repository.VehicleRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final VehicleRepository vehicleRepository;

    public CatalogService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public Page<Vehicle> browse(Long categoryId, int page, String sort) {
        Sort order;
        if ("price".equals(sort)) {
            order = Sort.by("pricePerDay").ascending();
        } else {
            order = Sort.by("id").descending();
        }

        Pageable pageable = PageRequest.of(Math.max(page, 0), 30, order);
        if (categoryId != null) {
            return vehicleRepository.findByActiveTrueAndCategory_Id(categoryId, pageable);
        }
        return vehicleRepository.findByActiveTrue(pageable);
    }

    public Page<Vehicle> search(String keyword, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), 30,
                Sort.by("id").descending());
        String safeKeyword = keyword == null ? "" : keyword.trim();
        return vehicleRepository.search(safeKeyword, pageable);
    }

    @Transactional
    public Vehicle detail(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .filter(Vehicle::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe."));
        vehicle.setViewCount(vehicle.getViewCount() + 1);
        return vehicle;
    }
}
