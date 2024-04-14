package com.ezone.ezproject.common.template;

import groovy.text.Template;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * https://docs.groovy-lang.org/latest/html/documentation/markup-template-engine.html#_basics
 */
@Slf4j
public class MarkupTemplate {
    private static final MarkupTemplateEngine ENGINE = new MarkupTemplateEngine(new TemplateConfiguration());

    public static String render(Map<String, Object> model, String templateText) throws Exception {
        Template template = ENGINE.createTemplate(templateText);
        StringWriter writer = new StringWriter();
        template.make(model).writeTo(writer);
        return writer.toString();
    }

    public static String render(String name, Object object, String templateText) throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put(name, object);
        return render(model, templateText);
    }

    public static String render(Object object, String templateText) throws Exception {
        String name = StringUtils.substringAfterLast(object.getClass().getName(), ".");
        Map<String, Object> model = new HashMap<>();
        model.put(toLowerCaseForFirstChar(name), object);
        return render(model, templateText);
    }

    public static String templateText(Class clazz, String suffix) throws Exception {
        return IOUtils.toString(clazz.getResource(clazz.getSimpleName() + suffix), "UTF-8");
    }

    public static String templateXml(Class clazz) throws Exception {
        return templateText(clazz, ".xml");
    }

    public static String templateHtml(Class clazz) throws Exception {
        return templateText(clazz, ".html");
    }

    public static String toLowerCaseForFirstChar(String s) {
        if (Character.isLowerCase(s.charAt(0))) {
            return s;
        } else {
            return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
        }
    }
}

