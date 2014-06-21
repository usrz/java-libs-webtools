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
package org.usrz.libs.webtools.mustache;

import static org.usrz.libs.utils.Check.notNull;

import java.io.IOException;
import java.io.Writer;
import java.util.Map.Entry;

import org.usrz.libs.webtools.resources.Resources;
import org.usrz.libs.webtools.templates.Template;
import org.usrz.libs.webtools.templates.TemplateException;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;

public class MustacheTemplate implements Template {

    private final MustacheTemplateManager factory;
    private final String name;
    private Mustache mustache;
    private Resources resources;

    protected MustacheTemplate(MustacheTemplateManager factory, String name) {
        this.factory = notNull(factory, "Null factory");
        this.name = notNull(name, "Null resource name");
        compile();
    }

    @Override
    public void execute(Writer output, Object scope)
    throws IOException, TemplateException {
        if (resources.hasChanged()) compile();
        try {
            mustache.execute(output, scope);
        } catch (MustacheException exception) {
            final Throwable cause = exception.getCause();
            if ((cause != null) && (cause instanceof IOException)) throw (IOException) cause;
            throw new TemplateException("Exception rendering Mustache", exception);
        }
    }

    private void compile() {
        try {
            final Entry<Mustache, Resources> compiled = factory.compileTemplate(name);
            mustache = compiled.getKey();
            resources = compiled.getValue();
        } catch (MustacheException exception) {
            throw new TemplateException("Exception compiling Mustache", exception);
        }
    }
}
