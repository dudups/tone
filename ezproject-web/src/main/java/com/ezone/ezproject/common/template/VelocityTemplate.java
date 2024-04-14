package com.ezone.ezproject.common.template;

import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;

/**
 * https://velocity.apache.org/engine/1.7/developer-guide.html#Configuring_Resource_Loaders
 */
@Slf4j
public class VelocityTemplate {
    private static final VelocityEngine ENGINE = new VelocityEngine();
    static {
        ENGINE.setProperty(VelocityEngine.RESOURCE_LOADERS, VelocityEngine.RESOURCE_LOADER_CLASS);
        ENGINE.setProperty("resource.loader.class.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        ENGINE.setProperty(VelocityEngine.RESOURCE_LOADER_CACHE, true);
        ENGINE.setProperty(VelocityEngine.RESOURCE_LOADER_CHECK_INTERVAL, 0);
        ENGINE.init();
    }

    public static String render(VelocityContext context, String resource) {
        Template template = ENGINE.getTemplate(resource);
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }

    public static String render(String name, Object object, String resource) {
        VelocityContext context = new VelocityContext();
        context.put(name, object);
        return render(context, resource);
    }
}

