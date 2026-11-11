package com.ieltspath.notification.application;

/** A fully rendered email: subject line, HTML body and plain-text body. Both bodies are always present. */
public record RenderedMail(String subject, String html, String text) { }
