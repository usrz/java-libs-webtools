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
package org.usrz.libs.webtools.templates;

import static org.usrz.libs.utils.Check.notNull;

import java.io.Writer;
import java.util.Map.Entry;

import org.usrz.libs.webtools.resources.Resources;

import com.github.mustachejava.Mustache;

public class ReloadingMustacheTemplate implements CompiledTemplate {

    private final ReloadingMustacheFactory factory;
    private final String name;
    private Mustache mustache;
    private Resources resources;

    protected ReloadingMustacheTemplate(ReloadingMustacheFactory factory, String name) {
        this.factory = notNull(factory, "Null factory");
        this.name = notNull(name, "Null resource name");
        compile();
    }

    @Override
    public void execute(Writer output, Object scope) {
        if (resources.hasChanged()) compile();
        mustache.execute(output, scope);
    }

    private void compile() {
        final Entry<Mustache, Resources> compiled = factory.compileTemplate(name);
        mustache = compiled.getKey();
        resources = compiled.getValue();
    }
}
