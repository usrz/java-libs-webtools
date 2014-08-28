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

import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

/**
 * A simple wrapper for <a href="http://lesscss.org/">LessCSS</a>.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
public class LessCSS {

    private static final String ENGINE_TYPE = "application/javascript";
    private static final String LESS_RESOURCE = "less-rhino-1.7.4.js";
    private static final String ADAPTER_RESOURCE = "less-adapter.js";

    private final ThreadLocal<Resource> resources = new ThreadLocal<>();

    private final ScriptEngineManager manager = new ScriptEngineManager(this.getClass().getClassLoader());
    private final ScriptEngine engine = manager.getEngineByMimeType(ENGINE_TYPE);
    private final Invocable invocable = (Invocable) engine;
    private final Log log = new Log();

    /**
     * Create a new {@link LessCSS} engine.
     */
    public LessCSS() {
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
        engine.getBindings(ScriptContext.ENGINE_SCOPE).put("_less_file_getter",

                /* Use lamba, easier */
                (Function<String, String>) (file) -> {

                    /* Check that we have a resource useable to resolve relative files */
                    final Resource originalResource = resources.get();
                    if (originalResource == null) {
                        log.warn("Unable to @import \"%s\" when converting a string", file);
                        return null;
                    }

                    /* Less already "resolves" path names for us, use the manager */
                    final Resource resource = originalResource.getResourceManager().getResource(file);
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
                });
    }

    /**
     * Convert the specified <em>LessCSS</em> source into a <em>CSS</em>
     * optionally compressing it.
     */
    public String convert(String less, boolean compress) {
        if (less == null) return null;

        try {
            final Map<String, Object> options = singletonMap("compress", compress);
            final Object css = invocable.invokeFunction("_less_process", less, options);
            return css == null ? null : css instanceof String ? (String) css : css.toString();
        } catch (Exception exception) {
            throw new LessCSSException("Unable to convert LESS script", exception);
        }
    }

    /**
     * Convert the specified <em>LessCSS</em> source file into a <em>CSS</em>
     * optionally compressing it.
     */
    public String convert(Resource resource, boolean compress) {

        /* Be kind if the resource does not exist */
        if (resource == null) return null;
        final String less = resource.readString();
        if (less == null) return null;

        /* Create our map of options */
        final Map<String, Object> options = new HashMap<>();
        options.put("filename", resource.getPath());
        options.put("compress", compress);

        /* Remember our resource in the threadlocal */
        resources.set(resource);

        try {
            /* Go! */
            final Object css = invocable.invokeFunction("_less_process", less, options);
            return css == null ? null : css instanceof String ? (String) css : css.toString();
        } catch (Exception exception) {
            throw new LessCSSException("Unable to convert LESS script at " + resource.getFile().getAbsolutePath(), exception);
        } finally {
            resources.remove();
        }
    }
}
