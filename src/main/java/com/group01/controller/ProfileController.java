package com.group01.controller;

import com.group01.entity.User;
import com.group01.service.AuthService;
import com.group01.service.CurrentMemberService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.security.Principal;

@Controller
public class ProfileController {

    private final AuthService authService;
    private final CurrentMemberService currentMember;

    public ProfileController(AuthService authService, CurrentMemberService currentMember) {
        this.authService = authService;
        this.currentMember = currentMember;
    }

    @GetMapping({"/profile", "/account/profile"})
    public String showProfilePage(Principal principal, Model model) {
        model.addAttribute("user", authService.findById(currentMember.requireMemberId(principal)));
        return "account/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam(defaultValue = "") String fullName,
                                @RequestParam(defaultValue = "") String phone,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
                                @RequestParam(defaultValue = "") String gender,
                                @RequestParam(defaultValue = "") String address,
                                @RequestParam(defaultValue = "") String licenseNumber,
                                Principal principal,
                                Model model) {
        Long userId = currentMember.requireMemberId(principal);

        try {
            User updatedUser = authService.updateProfile(userId, fullName, phone, dateOfBirth, gender, address, licenseNumber);
            model.addAttribute("user", updatedUser);
            model.addAttribute("successMessage", "Profile updated successfully.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("user", authService.findById(userId));
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "account/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam(defaultValue = "") String currentPassword,
                                 @RequestParam(defaultValue = "") String newPassword,
                                 @RequestParam(defaultValue = "") String confirmPassword,
                                 Principal principal,
                                 Model model) {
        Long userId = currentMember.requireMemberId(principal);

        try {
            authService.changePassword(userId, currentPassword, newPassword, confirmPassword);
            model.addAttribute("passwordSuccess", "Password changed successfully.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("passwordError", e.getMessage());
        }

        model.addAttribute("user", authService.findById(userId));
        return "account/profile";
    }
}
