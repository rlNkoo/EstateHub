package com.rlnkoo.notificationservice.mail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class MailTemplateServiceTest {

    @Autowired
    private MailTemplateService mailTemplateService;

    @Test
    void shouldRenderUserRegisteredTemplateWithProvidedModel() {
        // given
        Map<String, Object> model = Map.of(
                "email", "user@example.com",
                "activationToken", "activation-token-123"
        );

        // when
        String html = mailTemplateService.render(EmailTemplates.USER_REGISTERED, model);

        // then
        assertTrue(html.contains("user@example.com"));
        assertTrue(html.contains("activation-token-123"));
        assertTrue(html.contains("EstateHub"));
    }

    @Test
    void shouldRenderListingPublishedTemplateWithProvidedModel() {
        // given
        UUID listingId = UUID.randomUUID();
        Instant publishedAt = Instant.parse("2025-01-10T12:30:00Z");
        String listingLink = "http://localhost:8085/listings/" + listingId;

        Map<String, Object> model = Map.of(
                "email", "owner@example.com",
                "listingId", listingId,
                "title", "Beautiful apartment",
                "publishedAt", publishedAt,
                "listingLink", listingLink
        );

        // when
        String html = mailTemplateService.render(EmailTemplates.LISTING_PUBLISHED, model);

        // then
        assertTrue(html.contains("owner@example.com"));
        assertTrue(html.contains(listingId.toString()));
        assertTrue(html.contains("Beautiful apartment"));
        assertTrue(html.contains(publishedAt.toString()));
        assertTrue(html.contains(listingLink));
        assertTrue(html.contains("EstateHub"));
    }
}