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
package org.usrz.libs.webtools.uglifyjs;

import static org.usrz.libs.utils.Charsets.UTF8;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.usrz.libs.logging.Log;
import org.usrz.libs.logging.Logging;

/**
 * A simple wrapper for <a href="https://github.com/mishoo/UglifyJS2">UglifyJS
 * 2.x</a>.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
public class UglifyJS {

    private static final String ENGINE_TYPE = "application/javascript";
    private static final String UGLIFY_RESOURCE = "uglifyjs-2.4.12.min.js";
    private static final String ADAPTER_RESOURCE = "uglifyjs-adapter.js";

    private final ScriptEngineManager manager = new ScriptEngineManager(this.getClass().getClassLoader());
    private final ScriptEngine engine = manager.getEngineByMimeType(ENGINE_TYPE);
    private final Invocable invocable = (Invocable) engine;

    /**
     * Create a new {@link UglifyJS} engine.
     */
    public UglifyJS() {
        Logging.init();
        try {
            engine.put(ScriptEngine.FILENAME, UGLIFY_RESOURCE);
            final InputStream uglifyInput = this.getClass().getResourceAsStream(UGLIFY_RESOURCE);
            if (uglifyInput == null) throw new IOException("Resource " + UGLIFY_RESOURCE + " not found");
            final InputStreamReader lessReader = new InputStreamReader(uglifyInput, UTF8);
            engine.eval(lessReader);
            lessReader.close();

            engine.put(ScriptEngine.FILENAME, ADAPTER_RESOURCE);
            final InputStream adapterInput = this.getClass().getResourceAsStream(ADAPTER_RESOURCE);
            if (adapterInput == null) throw new IOException("Resource " + ADAPTER_RESOURCE + " not found");
            final InputStreamReader adapterReader = new InputStreamReader(adapterInput, UTF8);
            engine.eval(adapterReader);
            adapterReader.close();

            engine.getBindings(ScriptContext.GLOBAL_SCOPE).put("__logger", new Log());
        } catch (Exception exception) {
            throw new UglifyJSException("Unable to initialize UglifyJS2 engine", exception);
        }
    }

    /**
     * Uglify the specified <em>JavaScript</em> source.
     */
    public String convert(String script, boolean compress, boolean mangle) {
        final Map<String, Object> options = new HashMap<>();
        options.put("compress", compress);
        options.put("mangle", mangle);

        try {
            return invocable.invokeFunction("_uglify_process", script, options).toString();
        } catch (Exception exception) {
            throw new UglifyJSException("Unable to uglify script", exception);
        }
    }

}
