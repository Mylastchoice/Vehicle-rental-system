package com.group01.controller;

import com.group01.entity.Category;
import com.group01.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** M5 administration endpoints. All routes are protected by ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService service;
    public AdminController(AdminService service) { this.service = service; }

    @GetMapping("/users")
    public List<Map<String,Object>> users() { return service.listUsers(); }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> createUser(@RequestBody AdminService.UserInput input) {
        return service.createUser(input);
    }

    @PatchMapping("/users/{id}/status")
    public Map<String,Object> updateUserStatus(@PathVariable Long id, @RequestBody Map<String,String> body) {
        return service.updateUserStatus(id, body.get("status"));
    }

    @GetMapping("/vehicles")
    public List<Map<String,Object>> vehicles() { return service.listVehicles(); }

    @PostMapping("/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> createVehicle(@RequestBody AdminService.VehicleInput input) {
        return service.saveVehicle(null, input);
    }

    @PutMapping("/vehicles/{code}")
    public Map<String,Object> updateVehicle(@PathVariable String code,
                                             @RequestBody AdminService.VehicleInput input) {
        return service.saveVehicle(code, input);
    }

    @DeleteMapping("/vehicles/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVehicle(@PathVariable String code) { service.deleteVehicle(code); }

    @GetMapping("/categories")
    public List<Map<String,Object>> categories() {
        return service.listCategories().stream().map(AdminController::categoryMap).toList();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> createCategory(@RequestBody AdminService.CategoryInput input) {
        return categoryMap(service.saveCategory(null, input));
    }

    @PutMapping("/categories/{id}")
    public Map<String,Object> updateCategory(@PathVariable Long id,
                                              @RequestBody AdminService.CategoryInput input) {
        return categoryMap(service.saveCategory(id, input));
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) { service.deleteCategory(id); }

    private static Map<String,Object> categoryMap(Category c) {
        return Map.of("categoryId", c.getCategoryId(), "categoryName", c.getCategoryName(),
                "description", c.getDescription() == null ? "" : c.getDescription());
    }
}
