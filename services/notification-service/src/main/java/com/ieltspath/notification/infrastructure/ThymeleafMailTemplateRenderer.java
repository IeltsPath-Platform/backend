package com.ieltspath.notification.infrastructure;

import com.ieltspath.notification.application.MailTemplate;
import com.ieltspath.notification.application.MailTemplateRenderer;
import com.ieltspath.notification.application.RenderedMail;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Renders {@code mail-templates/<key>/subject.txt}, {@code body.html} and {@code body.txt}. HTML output escapes every
 * variable by default; plain text is not escaped because it is not markup.
 */
@Component
public class ThymeleafMailTemplateRenderer implements MailTemplateRenderer {
    private static final String ROOT = "mail-templates/";

    private final TemplateEngine htmlEngine = engine(TemplateMode.HTML, ".html");
    private final TemplateEngine textEngine = engine(TemplateMode.TEXT, ".txt");

    private static TemplateEngine engine(TemplateMode mode, String suffix) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix(ROOT);
        resolver.setSuffix(suffix);
        resolver.setTemplateMode(mode);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCheckExistence(true);
        TemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Override
    public RenderedMail render(MailTemplate template, Map<String, Object> model) {
        for (String variable : template.variables()) {
            if (model.get(variable) == null) {
                throw new IllegalArgumentException("Missing mail variable " + variable + " for template " + template.key());
            }
        }
        Context context = new Context(Locale.forLanguageTag("vi-VN"), new HashMap<>(model));
        String subject = textEngine.process(template.key() + "/subject", context).trim();
        String html = htmlEngine.process(template.key() + "/body", context);
        String text = textEngine.process(template.key() + "/body", context);
        return new RenderedMail(subject, html, text);
    }
}
