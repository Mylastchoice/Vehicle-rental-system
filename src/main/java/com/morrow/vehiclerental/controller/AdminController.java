package com.morrow.vehiclerental.controller;

import com.morrow.vehiclerental.model.*;
import com.morrow.vehiclerental.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/vehicles")
    public String vehicles(Model model) {
        model.addAttribute("vehicles", admin.allVehicles());
        return "admin/vehicles";
    }

    @GetMapping("/vehicles/new")
    public String newVehicle(Model model) {
        model.addAttribute("vehicle", new Vehicle());
        model.addAttribute("categories", admin.allCategories());
        return "admin/vehicle-form";
    }

    @PostMapping("/vehicles")
    public String createVehicle(@Valid @ModelAttribute Vehicle vehicle,
                                @RequestParam(required = false) Long categoryId,
                                RedirectAttributes redirect) {
        try {
            admin.saveVehicle(vehicle, categoryId);
            redirect.addFlashAttribute("success", "Đã thêm xe thành công.");
            return "redirect:/admin/vehicles";
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/vehicles/new";
        }
    }

    @GetMapping("/vehicles/{id}/edit")
    public String editVehicle(@PathVariable Long id, Model model) {
        model.addAttribute("vehicle", admin.findVehicle(id));
        model.addAttribute("categories", admin.allCategories());
        return "admin/vehicle-form";
    }

    @PostMapping("/vehicles/{id}")
    public String updateVehicle(@PathVariable Long id, @Valid @ModelAttribute Vehicle form,
                                @RequestParam(required = false) Long categoryId,
                                RedirectAttributes redirect) {
        try {
            Vehicle old = admin.findVehicle(id);
            form.setId(id);
            form.setViewCount(old.getViewCount());
            form.setActive(old.isActive());
            admin.saveVehicle(form, categoryId);
            redirect.addFlashAttribute("success", "Đã cập nhật xe.");
            return "redirect:/admin/vehicles";
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/vehicles/" + id + "/edit";
        }
    }

    @PostMapping("/vehicles/{id}/delete")
    public String deleteVehicle(@PathVariable Long id, RedirectAttributes redirect) {
        admin.deleteVehicle(id);
        redirect.addFlashAttribute("success", "Xe đã được chuyển sang trạng thái ngừng hoạt động.");
        return "redirect:/admin/vehicles";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", admin.allUsers());
        return "admin/users";
    }

    @GetMapping("/users/new")
    public String newUser(Model model) {
        model.addAttribute("user", new AppUser());
        return "admin/user-form";
    }

    @PostMapping("/users")
    public String createUser(@ModelAttribute("user") AppUser user,
                             @RequestParam String password,
                             RedirectAttributes redirect) {
        try {
            admin.saveUser(user, password);
            redirect.addFlashAttribute("success", "Đã thêm người dùng.");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/users/new";
        }
    }

    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) {
        model.addAttribute("user", admin.findUser(id));
        return "admin/user-form";
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute AppUser user,
                             RedirectAttributes redirect) {
        try {
            user.setId(id);
            admin.saveUser(user, null);
            redirect.addFlashAttribute("success", "Đã cập nhật người dùng.");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
    }

    @PostMapping("/users/{id}/lock")
    public String lockUser(@PathVariable Long id) {
        admin.setLocked(id, true);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/unlock")
    public String unlockUser(@PathVariable Long id) {
        admin.setLocked(id, false);
        return "redirect:/admin/users";
    }
}
