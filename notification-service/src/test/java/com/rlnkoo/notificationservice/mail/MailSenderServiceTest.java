package com.rlnkoo.notificationservice.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailSenderServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailSenderService mailSenderService;

    @Test
    void shouldCreateMimeMessageAndSendHtmlEmail() throws Exception {
        // given
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        setField(mailSenderService, "from", "no-reply@estatehub");

        String to = "user@example.com";
        String subject = "Test subject";
        String html = "<html><body>Test</body></html>";

        // when
        mailSenderService.sendHtml(to, subject, html);

        // then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);

        assertEquals("Test subject", mimeMessage.getSubject());
        assertEquals("no-reply@estatehub", mimeMessage.getFrom()[0].toString());
        assertEquals("user@example.com", mimeMessage.getAllRecipients()[0].toString());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenMailSenderFails() {
        // given
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP error"));

        setField(mailSenderService, "from", "no-reply@estatehub");

        // when + then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> mailSenderService.sendHtml("user@example.com", "subject", "<html></html>")
        );

        assertTrue(exception.getMessage().contains("Cannot send email"));
        verify(mailSender).createMimeMessage();
        verify(mailSender, never()).send((MimeMessage) any());
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}