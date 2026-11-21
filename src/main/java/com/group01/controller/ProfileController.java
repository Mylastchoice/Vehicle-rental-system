package com.group01.controller;

import com.group01.entity.User;
import com.group01.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class ProfileController {

    private final AuthService authService;

    public ProfileController(AuthService authService) {
        this.authService = authService;
    }

    private String getBaseUrl(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedHost != null && !forwardedHost.isEmpty()) {
            String host = forwardedHost.split(",")[0].trim();
            String scheme = (forwardedProto != null && !forwardedProto.isEmpty())
                    ? forwardedProto.split(",")[0].trim()
                    : request.getScheme();
            return scheme + "://" + host;
        }
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        if ((scheme.equals("http") && serverPort == 80) || (scheme.equals("https") && serverPort == 443)) {
            return scheme + "://" + serverName;
        }
        return scheme + "://" + serverName + ":" + serverPort;
    }

    @GetMapping({"/profile", "/account/profile"})
    public String showProfilePage(HttpSession session, HttpServletRequest request, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:" + getBaseUrl(request) + "/login";
        }

        User user = authService.findById(userId);
        if (user == null) {
            session.invalidate();
            return "redirect:" + getBaseUrl(request) + "/login";
        }

        model.addAttribute("user", user);
        return "account/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam(defaultValue = "") String fullName,
                                @RequestParam(defaultValue = "") String phone,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
                                @RequestParam(defaultValue = "") String gender,
                                @RequestParam(defaultValue = "") String address,
                                @RequestParam(defaultValue = "") String licenseNumber,
                                HttpSession session,
                                HttpServletRequest request,
                                Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:" + getBaseUrl(request) + "/login";
        }

        try {
            User updatedUser = authService.updateProfile(userId, fullName, phone, dateOfBirth, gender, address, licenseNumber);
            session.setAttribute("loggedInUser", updatedUser);
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
                                 HttpSession session,
                                 HttpServletRequest request,
                                 Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:" + getBaseUrl(request) + "/login";
        }

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