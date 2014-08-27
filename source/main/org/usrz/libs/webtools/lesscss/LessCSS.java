/* ========================================================================== *
 * Copyright 2014 USRZ.com and Pier Paolo Fumagalli                           *
 * -------------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *  http://www.apache.org/licenses/LICENSE-2.0                                *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 * ========================================================================== */
package org.usrz.libs.webtools.lesscss;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.usrz.libs.logging.Log;
import org.usrz.libs.utils.Charsets;
import org.usrz.libs.webtools.resources.Resource;
import org.usrz.libs.webtools.resources.ResourceManager;

/**
 * A simple wrapper for <a href="http://lesscss.org/">LessCSS</a>.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
public class LessCSS {

    private static final String ENGINE_TYPE = "application/javascript";
    private static final String LESS_RESOURCE = "less-rhino-1.7.4.js";
    private static final String ADAPTER_RESOURCE = "less-adapter.js";

    private final ScriptEngineManager manager = new ScriptEngineManager(this.getClass().getClassLoader());
    private final ScriptEngine engine = manager.getEngineByMimeType(ENGINE_TYPE);
    private final Invocable invocable = (Invocable) engine;
    private final Function<String, String> fileGetter;
    private final Log log = new Log();

    /**
     * Create a new {@link LessCSS} engine.
     */
    public LessCSS() {
        this(null);
    }

    /**
     * Create a new {@link LessCSS} engine using the specified
     * {@link ResourceManager} for importing extra content.
     */
    public LessCSS(ResourceManager manager) {
        try {
            engine.put(ScriptEngine.FILENAME, LESS_RESOURCE);
            final InputStream lessInput = this.getClass().getResourceAsStream(LESS_RESOURCE);
            if (lessInput == null) throw new IOException("Resource " + LESS_RESOURCE + " not found");
            engine.eval(new InputStreamReader(lessInput, Charsets.UTF8));

            engine.put(ScriptEngine.FILENAME, ADAPTER_RESOURCE);
            final InputStream adapterInput = this.getClass().getResourceAsStream(ADAPTER_RESOURCE);
            if (adapterInput == null) throw new IOException("Resource " + ADAPTER_RESOURCE + " not found");
            engine.eval(new InputStreamReader(adapterInput, Charsets.UTF8));
        } catch (Exception exception) {
            throw new LessCSSException("Unable to initialize LESS engine", exception);
        }

        /* Our file getter for @include */
        fileGetter = manager == null ? null : (file) -> {
            final Resource resource = manager.getResource(file);
            if (resource != null) {
                final String less = resource.readString();
                if (less != null) {
                    log.debug("Resource \"%s\" imported from \"%s\"", file, resource.getFile().getAbsolutePath());
                } else {
                    log.debug("Resource \"%s\" could not be read from \"%s\"", file, resource.getFile().getAbsolutePath());
                }
                return less;
            } else {
                log.debug("Resource \"%s\" not found");
                return null;
            }
        };
        engine.getBindings(ScriptContext.ENGINE_SCOPE).put("_less_file_getter", fileGetter);
    }

    /**
     * Convert the specified <em>LessCSS</em> source into a <em>CSS</em>
     * optionally compressing it.
     */
    public String convert(String less, boolean compress) {
        return lessc(less, Collections.singletonMap("compress", compress));
    }

    /**
     * Convert the specified <em>LessCSS</em> source file into a <em>CSS</em>
     * optionally compressing it.
     */
    public String parse(String fileName, boolean compress) {
        final String less = fileGetter.apply(fileName);
        if (less == null) return null;

        final Map<String, Object> options = new HashMap<>();
        options.put("compress", compress);
        options.put("filename", fileName);

        return lessc(less, options);
    }

    protected String lessc(String less, Map<String, Object> options) {
        try {
            return invocable.invokeFunction("_less_process", less, options).toString();
        } catch (Exception exception) {
            final Object fileName = options.get("filename");
            if (fileName != null) {
                throw new LessCSSException("Unable to convert LESS script at " + fileName, exception);
            } else {
                throw new LessCSSException("Unable to convert inline LESS script", exception);
            }
        }
    }
}
