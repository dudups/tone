package com.ezone.ezproject.common.template;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import javax.script.Compilable;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * https://docs.oracle.com/javase/10/nashorn/introduction.htm#JSNUG136 jdk 8-10 nashorn语法相同
 * https://www.baeldung.com/java-nashorn
 */
@Slf4j
public class JsTemplate {
    private static final Map<String, Render> RENDER_CACHE = new HashMap<>();

    public static Render render(String resource) throws Exception {
        Render render = RENDER_CACHE.get(resource);
        if (null == render) {
            String js = IOUtils.toString(JsTemplate.class.getResource(resource), Charset.forName("UTF-8"));
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
            ((Compilable) engine).compile(js).eval();
            render = ((Invocable) engine).getInterface(Render.class);
            RENDER_CACHE.put(resource, render);
        }
        return render;
    }

    public interface Render {
        String render(Object object);
    }
}

