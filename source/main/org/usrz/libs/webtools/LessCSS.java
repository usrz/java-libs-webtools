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
import java.util.Collections;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.usrz.libs.utils.Charsets;

public class LessCSS {

    private static final String ENGINE_TYPE = "application/javascript";
    private static final String LESS_RESOURCE = "less-rhino-1.7.0.js";
    private static final String ADAPTER_RESOURCE = "less-adapter.js";

    private final ScriptEngineManager manager = new ScriptEngineManager(this.getClass().getClassLoader());
    private final ScriptEngine engine = manager.getEngineByMimeType(ENGINE_TYPE);
    private final Invocable invocable = (Invocable) engine;

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
    }

    public String convert(String less, boolean compress) {
        final Map<String, Object> options = Collections.singletonMap("compress", compress);
        try {
            return invocable.invokeFunction("_less_process", less, options).toString();
        } catch (Exception exception) {
            throw new LessCSSException("Unable to convert LESS script", exception);
        }
    }

}
