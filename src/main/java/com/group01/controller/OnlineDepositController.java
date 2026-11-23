package com.group01.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.service.CurrentMemberService;
import com.group01.service.OnlineDepositService;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OnlineDepositController {
    private final OnlineDepositService payments;
    private final CurrentMemberService members;
    private final ObjectMapper json;
    private final String webhookSecret;

    public OnlineDepositController(OnlineDepositService payments, CurrentMemberService members, ObjectMapper json,
                                   @Value("${morrow.stripe.webhook-secret:}") String webhookSecret) {
        this.payments = payments;
        this.members = members;
        this.json = json;
        this.webhookSecret = webhookSecret;
    }

    @GetMapping("/api/payment-options")
    @ResponseBody
    public Map<String, Boolean> options() {
        return Map.of("online", payments.enabled());
    }

    @PostMapping("/bookings/{id}/payment")
    public Object open(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        Long memberId = members.requireMemberId(principal);
        try {
            URI checkout = payments.open(id, memberId);
            return ResponseEntity.status(HttpStatus.SEE_OTHER).location(checkout).build();
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) throw e;
            flash.addFlashAttribute("message", e.getReason());
            return "redirect:/bookings/" + id;
        }
    }

    @GetMapping("/bookings/{id}/payment/return")
    public String returned(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        Long memberId = members.requireMemberId(principal);
        try {
            boolean paid = payments.reconcile(id, memberId);
            flash.addFlashAttribute("message", paid ? "Sandbox deposit paid." : "Deposit payment is pending.");
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) throw e;
            flash.addFlashAttribute("message", e.getReason());
        }
        return "redirect:/bookings/" + id;
    }

    @PostMapping("/api/payments/stripe/webhook")
    public ResponseEntity<Void> webhook(@RequestHeader("Stripe-Signature") String signature, @RequestBody byte[] body)
            throws IOException, GeneralSecurityException {
        if (webhookSecret.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        if (!validSignature(signature, body)) {
            return ResponseEntity.badRequest().build();
        }
        var event = json.readTree(body);
        String sessionId = event.path("data").path("object").path("id").asText();
        if ("checkout.session.completed".equals(event.path("type").asText()) &&
                !event.path("livemode").asBoolean(true) && sessionId.startsWith("cs_test_")) {
            payments.reconcileEvent(sessionId);
        }
        return ResponseEntity.noContent().build();
    }

    private boolean validSignature(String header, byte[] body) throws GeneralSecurityException {
        String timestamp = "";
        var signatures = new ArrayList<String>();
        for (String field : header.split(",")) {
            String[] pair = field.trim().split("=", 2);
            if (pair.length != 2) continue;
            if ("t".equals(pair[0])) timestamp = pair[1];
            if ("v1".equals(pair[0])) signatures.add(pair[1]);
        }
        if (!timestamp.matches("[0-9]{1,12}") ||
                Math.abs(Instant.now().getEpochSecond() - Long.parseLong(timestamp)) > 300) {
            return false;
        }
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
        byte[] expected = HexFormat.of().formatHex(mac.doFinal(body)).getBytes(StandardCharsets.US_ASCII);
        return signatures.stream().anyMatch(value ->
                MessageDigest.isEqual(expected, value.getBytes(StandardCharsets.US_ASCII)));
    }
}
