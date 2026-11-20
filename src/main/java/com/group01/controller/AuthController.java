package com.group01.controller;

import com.group01.entity.User;
import com.group01.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Controller
public class AuthController {

    private final AuthService authService;

    // Lưu danh sách thời điểm gửi request theo từng IP để chặn spam (tối đa 5 lần / 10 phút)
    private final Map<String, List<Long>> requestLogs = new HashMap<>();

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    private boolean isSpamRequest(String ip) {
        long now = System.currentTimeMillis();
        long tenMinutes = 10 * 60 * 1000L;

        List<Long> times = requestLogs.getOrDefault(ip, new ArrayList<>());
        List<Long> recentTimes = new ArrayList<>();
        for (Long t : times) {
            if (now - t <= tenMinutes) {
                recentTimes.add(t);
            }
        }

        if (recentTimes.size() >= 5) {
            requestLogs.put(ip, recentTimes);
            return true;
        }

        recentTimes.add(now);
        requestLogs.put(ip, recentTimes);
        return false;
    }

    private void taoCaptcha(HttpSession session) {
        Random rd = new Random();
        int a = rd.nextInt(9) + 1;
        int b = rd.nextInt(9) + 1;
        session.setAttribute("captchaDapAn", String.valueOf(a + b));
        session.setAttribute("captchaCauHoi", a + " + " + b + " = ?");
    }

    @GetMapping("/register")
    public String showRegisterPage(HttpSession session, Model model) {
        taoCaptcha(session);
        model.addAttribute("captchaCauHoi", session.getAttribute("captchaCauHoi"));
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(@RequestParam(defaultValue = "") String fullName,
                                 @RequestParam(defaultValue = "") String phone,
                                 @RequestParam(defaultValue = "") String email,
                                 @RequestParam(defaultValue = "") String password,
                                 @RequestParam(defaultValue = "") String confirmPassword,
                                 @RequestParam(defaultValue = "") String captchaInput,
                                 HttpSession session,
                                 HttpServletRequest request,
                                 Model model) {
        if (isSpamRequest(request.getRemoteAddr())) {
            taoCaptcha(session);
            model.addAttribute("captchaCauHoi", session.getAttribute("captchaCauHoi"));
            model.addAttribute("serverError", "Too many requests. Please try again after 10 minutes.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("phone", phone);
            model.addAttribute("email", email);
            return "auth/register";
        }

        String dapAnDung = (String) session.getAttribute("captchaDapAn");
        if (dapAnDung == null || !dapAnDung.equals(captchaInput.trim())) {
            taoCaptcha(session);
            model.addAttribute("captchaCauHoi", session.getAttribute("captchaCauHoi"));
            model.addAttribute("serverError", "Wrong CAPTCHA answer.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("phone", phone);
            model.addAttribute("email", email);
            return "auth/register";
        }

        session.removeAttribute("captchaDapAn");
        session.removeAttribute("captchaCauHoi");

        try {
            authService.register(fullName, phone, email, password, confirmPassword);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            taoCaptcha(session);
            model.addAttribute("captchaCauHoi", session.getAttribute("captchaCauHoi"));
            model.addAttribute("serverError", e.getMessage());
            model.addAttribute("fullName", fullName);
            model.addAttribute("phone", phone);
            model.addAttribute("email", email);
            return "auth/register";
        }
    }

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(required = false) String registered,
                                @RequestParam(required = false) String resetSuccess,
                                Model model) {
        if ("true".equals(registered)) {
            model.addAttribute("successMessage", "Registration successful! Please log in.");
        }
        if ("true".equals(resetSuccess)) {
            model.addAttribute("successMessage", "Password reset successful! Please log in.");
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam(defaultValue = "") String email,
                              @RequestParam(defaultValue = "") String password,
                              HttpSession session,
                              Model model) {
        try {
            User user = authService.login(email, password);
            session.setAttribute("loggedInUser", user);
            session.setAttribute("userId", user.getUserId());
            return "redirect:/profile";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("serverError", e.getMessage());
            model.addAttribute("email", email);
            return "auth/login";
        }
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam(defaultValue = "") String email,
                                       HttpServletRequest request,
                                       Model model) {
        if (isSpamRequest(request.getRemoteAddr())) {
            model.addAttribute("emailError", "Too many requests. Please try again after 10 minutes.");
            return "auth/forgot-password";
        }

        try {
            String token = authService.createPasswordResetToken(email);
            model.addAttribute("successMessage",
                    "If the email exists, a password reset link (valid for 30 minutes) has been created.");
            if (token != null) {
                model.addAttribute("resetLink", "/reset-password?token=" + token);
            }
            return "auth/forgot-password";
        } catch (IllegalArgumentException e) {
            model.addAttribute("emailError", e.getMessage());
            model.addAttribute("email", email);
            return "auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam(defaultValue = "") String token, Model model) {
        model.addAttribute("token", token);
        if (!authService.isValidResetToken(token)) {
            model.addAttribute("tokenError", "Invalid or expired reset link.");
        }
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam(defaultValue = "") String token,
                                      @RequestParam(defaultValue = "") String newPassword,
                                      @RequestParam(defaultValue = "") String confirmPassword,
                                      Model model) {
        try {
            authService.resetPassword(token, newPassword, confirmPassword);
            return "redirect:/login?resetSuccess=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("token", token);
            model.addAttribute("serverError", e.getMessage());
            return "auth/reset-password";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}