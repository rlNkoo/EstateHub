package com.rlnkoo.notificationservice.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailTemplateService {

    private final SpringTemplateEngine templateEngine;

    public String render(String template, Map<String, Object> model) {
        Context ctx = new Context(Locale.forLanguageTag("pl"));
        ctx.setVariables(model);
        return templateEngine.process(template, ctx);
    }
}