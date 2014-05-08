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
package org.usrz.libs.webtools;

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
import org.usrz.libs.utils.Charsets;

public class UglifyJS2 {

    private static final String ENGINE_TYPE = "application/javascript";
    private static final String UGLIFY_RESOURCE = "uglifyjs-2.4.12.min.js";
    private static final String ADAPTER_RESOURCE = "uglifyjs-adapter.js";

    private final ScriptEngineManager manager = new ScriptEngineManager(this.getClass().getClassLoader());
    private final ScriptEngine engine = manager.getEngineByMimeType(ENGINE_TYPE);
    private final Invocable invocable = (Invocable) engine;

    public UglifyJS2() {
        Logging.init();
        try {
            engine.put(ScriptEngine.FILENAME, UGLIFY_RESOURCE);
            final InputStream lessInput = this.getClass().getResourceAsStream(UGLIFY_RESOURCE);
            if (lessInput == null) throw new IOException("Resource " + UGLIFY_RESOURCE + " not found");
            engine.eval(new InputStreamReader(lessInput, Charsets.UTF8));

            engine.put(ScriptEngine.FILENAME, ADAPTER_RESOURCE);
            final InputStream adapterInput = this.getClass().getResourceAsStream(ADAPTER_RESOURCE);
            if (adapterInput == null) throw new IOException("Resource " + ADAPTER_RESOURCE + " not found");
            engine.eval(new InputStreamReader(adapterInput, Charsets.UTF8));

            engine.getBindings(ScriptContext.GLOBAL_SCOPE).put("__logger", new Log());
        } catch (Exception exception) {
            throw new UglifyJS2Exception("Unable to initialize UglifyJS2 engine", exception);
        }
    }

    public String convert(String less, boolean compress, boolean mangle) {
        final Map<String, Object> options = new HashMap<>();
        options.put("compress", compress);
        options.put("mangle", mangle);

        try {
            return invocable.invokeFunction("_uglify_process", less, options).toString();
        } catch (Exception exception) {
            throw new UglifyJS2Exception("Unable to uglify script", exception);
        }
    }

}
