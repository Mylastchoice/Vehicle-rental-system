package com.group01.controller;

import com.group01.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String mailPassword;

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

    private void sendResetPasswordEmail(String toEmail, String resetUrl) {
        if (mailSender == null || fromEmail == null || fromEmail.isBlank()
                || mailPassword == null || mailPassword.isBlank()) {
            System.out.println("[DEV LOG] Reset password link for " + toEmail + ": " + resetUrl);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("[Morrow Vehicle Rental] Password Reset Request");
        message.setText("Hello,\n\n"
                + "We received a request to reset your Morrow account password.\n"
                + "Click the link below to set a new password (valid for 30 minutes, one-time use):\n"
                + resetUrl + "\n\n"
                + "If you did not request this, please ignore this email.");
        mailSender.send(message);
    }

    // ==================== 1. ĐĂNG KÝ TÀI KHOẢN (UC03) ====================

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
            return "redirect:" + getBaseUrl(request) + "/login?registered=true";
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

    // ==================== 2. ĐĂNG NHẬP & ĐĂNG XUẤT (UC04) ====================

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
                              HttpServletRequest request,
                              Model model) {
        try {
            UserDetails user = authService.login(email, password);
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    user.getUsername(), null, user.getAuthorities());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            request.changeSessionId();
            SecurityContextHolder.setContext(context);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            boolean admin = user.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));
            return admin ? "redirect:/admin/bookings" : "redirect:/";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("serverError", e.getMessage());
            model.addAttribute("email", email);
            return "auth/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        session.invalidate();
        return "redirect:" + getBaseUrl(request) + "/login";
    }

    // ==================== 3. QUÊN MẬT KHẨU & ĐẶT LẠI MẬT KHẨU (UC05) ====================

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

        String normalizedEmail = email.trim().toLowerCase();
        if (normalizedEmail.isEmpty() || !normalizedEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            model.addAttribute("emailError", "Please enter a valid email address.");
            model.addAttribute("email", email);
            return "auth/forgot-password";
        }

        String genericSuccess = "If an account with that email exists, a password reset link (valid for 30 minutes) has been sent to your email.";

        try {
            String token = authService.createPasswordResetToken(normalizedEmail);
            if (token != null) {
                String resetUrl = getBaseUrl(request) + "/reset-password?token=" + token;
                sendResetPasswordEmail(normalizedEmail, resetUrl);
            }
            model.addAttribute("successMessage", genericSuccess);
            return "auth/forgot-password";
        } catch (IllegalArgumentException e) {
            model.addAttribute("successMessage", genericSuccess);
            return "auth/forgot-password";
        } catch (Exception e) {
            model.addAttribute("emailError", "Unable to send reset email right now. Please try again later.");
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
                                      HttpServletRequest request,
                                      Model model) {
        try {
            authService.resetPassword(token, newPassword, confirmPassword);
            return "redirect:" + getBaseUrl(request) + "/login?resetSuccess=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("token", token);
            model.addAttribute("serverError", e.getMessage());
            return "auth/reset-password";
        }
    }
}
