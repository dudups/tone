package com.ezone.ezproject.modules.system.service;

import com.ezone.ezproject.common.exception.CodedException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.OutputStream;
import java.io.PrintWriter;

@Service
@Slf4j
@AllArgsConstructor
public class SystemService {
    public void runGroovy(String script, OutputStream out) {
        if (StringUtils.isEmpty(script)) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "script is empty!");
        }
        try (PrintWriter writer = new PrintWriter(out)) {
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("groovy");
            Bindings bindings = engine.createBindings();
            engine.getContext().setWriter(writer);
            engine.getContext().setErrorWriter(writer);
            engine.eval(script, bindings);
        } catch (Throwable e) {
            e.printStackTrace(new PrintWriter(out));
        }
    }
}
