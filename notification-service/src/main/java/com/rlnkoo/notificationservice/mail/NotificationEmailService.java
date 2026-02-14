package com.rlnkoo.notificationservice.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationEmailService {

    private final MailTemplateService templateService;
    private final MailSenderService senderService;

    public void send(EmailMessage message) {
        String html = templateService.render(message.template(), message.model());
        senderService.sendHtml(message.to(), message.subject(), html);
    }
}