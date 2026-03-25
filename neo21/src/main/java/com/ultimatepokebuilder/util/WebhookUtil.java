package com.ultimatepokebuilder.util;

import com.ultimatepokebuilder.UltimatePokeBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class WebhookUtil {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static void sendAudit(String webhookUrl, String message) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_HERE")) return;

        CompletableFuture.runAsync(() -> {
            try {
                String json = "{\"content\": \"" + message.replace("\"", "\\\"") + "\"}";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                UltimatePokeBuilder.LOGGER.error("Failed to send UPB Discord Webhook", e);
            }
        });
    }
}