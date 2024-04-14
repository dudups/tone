package com.ezone.ezproject.modules.hook.message;

import org.apache.commons.lang.StringEscapeUtils;

public interface WebHookMessageModel {
    String getTitle();

    default String getHtmlTitle() {
        return getTitle();
    }

    default String getMdTitle() {
        return getHtmlTitle();
    }

    String getContent();

    default String getHtmlContent() {
        return getContent();
    }

    default String getMdContent() {
        return getHtmlContent();
    }

    default String getRichTextContent() {
        return getHtmlContent();
    }

    default String getEscapeTitle() {
        return StringEscapeUtils.escapeJava(getTitle());
    }

    default String getEscapeHtmlTitle() {
        return StringEscapeUtils.escapeJava(getHtmlTitle());
    }

    default String getEscapeMdTitle() {
        return StringEscapeUtils.escapeJava(getMdTitle());
    }

    default String getEscapeContent() {
        return StringEscapeUtils.escapeJava(getContent());
    }

    default String getEscapeHtmlContent() {
        return StringEscapeUtils.escapeJava(getHtmlContent());
    }

    default String getEscapeMdContent() {
        return StringEscapeUtils.escapeJava(getMdContent());
    }

    default String getEscapeRichTextContent() {
        return StringEscapeUtils.escapeJava(getRichTextContent());
    }
}
