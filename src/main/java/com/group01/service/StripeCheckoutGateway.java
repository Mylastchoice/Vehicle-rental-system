package com.group01.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class StripeCheckoutGateway {
    private final RestClient client;
    private final String key;
    private final String origin;

    public StripeCheckoutGateway(@Value("${morrow.stripe.secret-key:}") String key,
                                 @Value("${morrow.stripe.app-origin:http://localhost:8080}") String origin) {
        this.key = key;
        this.origin = origin;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client = RestClient.builder().baseUrl("https://api.stripe.com")
                .requestFactory(factory).build();
    }

    public boolean enabled() {
        return key.startsWith("sk_test_") && key.length() > "sk_test_".length();
    }

    public JsonNode create(long bookingId, long amount, String idempotencyKey) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("mode", "payment");
        form.add("payment_method_types[]", "card");
        form.add("line_items[0][quantity]", "1");
        form.add("line_items[0][price_data][currency]", "vnd");
        form.add("line_items[0][price_data][unit_amount]", Long.toString(amount));
        form.add("line_items[0][price_data][product_data][name]", "Morrow booking deposit #" + bookingId);
        form.add("success_url", origin + "/bookings/" + bookingId + "/payment/return");
        form.add("cancel_url", origin + "/bookings/" + bookingId);
        return read(client.post().uri("/v1/checkout/sessions")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form));
    }

    public JsonNode retrieve(String sessionId) {
        return read(client.get().uri("/v1/checkout/sessions/{id}", sessionId));
    }

    public void expire(String sessionId) {
        read(client.post().uri("/v1/checkout/sessions/{id}/expire", sessionId));
    }

    private JsonNode read(RestClient.RequestHeadersSpec<?> request) {
        if (!enabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe sandbox is not configured");
        }
        try {
            JsonNode result = request.headers(headers -> headers.setBearerAuth(key)).retrieve().body(JsonNode.class);
            if (result != null && result.path("livemode").isBoolean() && !result.path("livemode").asBoolean()) {
                return result;
            }
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe sandbox could not process the request", e);
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid Stripe sandbox response");
    }
}
