package com.rlnkoo.notificationservice.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEmailServiceTest {

    @Mock
    private MailTemplateService templateService;

    @Mock
    private MailSenderService senderService;

    @InjectMocks
    private NotificationEmailService notificationEmailService;

    @Test
    void shouldRenderTemplateAndSendHtmlEmail() {
        // given
        Map<String, Object> model = Map.of(
                "email", "user@example.com",
                "activationToken", "token-123"
        );

        EmailMessage message = new EmailMessage(
                "user@example.com",
                "Activate your EstateHub account",
                "user-registered",
                model
        );

        String html = "<html><body>Rendered email</body></html>";

        when(templateService.render("user-registered", model)).thenReturn(html);

        // when
        notificationEmailService.send(message);

        // then
        verify(templateService).render("user-registered", model);
        verify(senderService).sendHtml("user@example.com", "Activate your EstateHub account", html);
    }

    @Test
    void shouldPropagateExceptionWhenTemplateRenderingFails() {
        // given
        Map<String, Object> model = Map.of(
                "email", "user@example.com"
        );

        EmailMessage message = new EmailMessage(
                "user@example.com",
                "Subject",
                "template-name",
                model
        );

        RuntimeException exception = new RuntimeException("template rendering failed");

        when(templateService.render("template-name", model)).thenThrow(exception);

        // when + then
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> notificationEmailService.send(message)
        );

        org.junit.jupiter.api.Assertions.assertSame(exception, thrown);
        verify(templateService).render("template-name", model);
        verify(senderService, never()).sendHtml(anyString(), anyString(), anyString());
    }

    @Test
    void shouldPropagateExceptionWhenSendingHtmlEmailFails() {
        // given
        Map<String, Object> model = Map.of(
                "email", "user@example.com"
        );

        EmailMessage message = new EmailMessage(
                "user@example.com",
                "Subject",
                "template-name",
                model
        );

        String html = "<html><body>Email</body></html>";
        RuntimeException exception = new RuntimeException("smtp failed");

        when(templateService.render("template-name", model)).thenReturn(html);
        doThrow(exception).when(senderService).sendHtml("user@example.com", "Subject", html);

        // when + then
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> notificationEmailService.send(message)
        );

        org.junit.jupiter.api.Assertions.assertSame(exception, thrown);
        verify(templateService).render("template-name", model);
        verify(senderService).sendHtml("user@example.com", "Subject", html);
    }
}