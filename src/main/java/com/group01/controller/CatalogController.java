package com.group01.controller;

import com.group01.dto.BranchResponse;
import com.group01.dto.CategoryResponse;
import com.group01.dto.PageResponse;
import com.group01.dto.VehicleDetailResponse;
import com.group01.dto.VehicleSummaryResponse;
import com.group01.service.CatalogService;
import java.util.List;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public read-only catalogue (guests allowed, see SecurityConfig). */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /** GET /api/vehicles?categoryId=1&keyword=honda&status=Available&sort=price&order=desc&page=0&size=30 */
    @GetMapping("/vehicles")
    public PageResponse<VehicleSummaryResponse> vehicles(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) LocalDate pickupDate,
            @RequestParam(required = false) LocalDate returnDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return catalogService.searchVehicles(categoryId, keyword, status, sort, order, pickupDate, returnDate, page, size);
    }

    @GetMapping("/vehicles/{vehicleCode}")
    public VehicleDetailResponse vehicle(@PathVariable String vehicleCode) {
        catalogService.recordVehicleView(vehicleCode);
        return catalogService.getVehicle(vehicleCode);
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return catalogService.listCategories();
    }

    @GetMapping("/branches")
    public List<BranchResponse> branches() {
        return catalogService.listBranches();
    }
}
