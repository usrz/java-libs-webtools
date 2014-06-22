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

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;

import org.usrz.libs.utils.Check;
import org.usrz.libs.webtools.templates.Template;
import org.usrz.libs.webtools.templates.TemplateException;
import org.usrz.libs.webtools.templates.TemplateFactory;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;

public class MustacheTemplateFactory implements TemplateFactory {

    protected final MustacheFactory factory;

    public MustacheTemplateFactory() {
        this(new DefaultMustacheFactory());
    }

    protected MustacheTemplateFactory(MustacheFactory factory) {
        this.factory = Check.notNull(factory, "Null Mustache Factory");
    }

    @Override
    public final Template parse(String template) {
        final StringReader reader = new StringReader(template);
        final Mustache mustache = factory.compile(reader, null);
        return new Template() {

            @Override
            public void execute(Writer output, Object scope)
            throws IOException, TemplateException {
                try {
                    mustache.execute(output, scope);
                } catch (MustacheException exception) {
                    final Throwable cause = exception.getCause();
                    if ((cause != null) && (cause instanceof IOException)) throw (IOException) cause;
                    throw new TemplateException("Exception rendering Mustache", exception);
                }
            }

        };
    }

}
