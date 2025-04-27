package com.education.amenity.management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class BotNotificationService {

    private final RestTemplate restTemplate;
    private final String botWebHookUrl;

    @Autowired
    public BotNotificationService(RestTemplate restTemplate,
                                  @Value("${bot.webhook.url}") String botWebHookUrl) {
        this.restTemplate = restTemplate;
        this.botWebHookUrl = botWebHookUrl;
    }

    public void notifyBot(String studentId) {  // Changed from Long to String
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("studentId", studentId);
            payload.put("status", "Upload Success");
            payload.put("timestamp", Instant.now().toString());

            ResponseEntity<String> response = restTemplate.postForEntity(
                    botWebHookUrl,
                    payload,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Bot notification failed: " +
                        response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            // Use proper logging instead of System.err
            throw new RuntimeException("Failed to notify WhatsApp bot", e);
        }
    }
}