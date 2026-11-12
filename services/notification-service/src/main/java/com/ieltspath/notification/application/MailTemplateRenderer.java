package com.ieltspath.notification.application;

import java.util.Map;

/** Turns a template and its variables into a {@link RenderedMail}. Implementations refuse a missing variable. */
public interface MailTemplateRenderer {
    RenderedMail render(MailTemplate template, Map<String, Object> model);
}
