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
package org.usrz.libs.webtools.exceptions;

import static javax.ws.rs.Priorities.USER;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static org.usrz.libs.utils.Charsets.UTF8;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;

import org.usrz.libs.webtools.mustache.MustacheTemplateFactory;
import org.usrz.libs.webtools.templates.Template;
import org.usrz.libs.webtools.templates.TemplateFactory;
import org.usrz.libs.webtools.utils.EncodingMessageBodyWriter;

@Provider
@Singleton
@Priority(USER + 1001)
@Produces(TEXT_HTML)
public class HtmlExceptionWrapperBodyWriter extends EncodingMessageBodyWriter<ExceptionWrapper> {

    private static final String TEMPLATE_NAME = "exception_wrapper.mustache";
    private final TemplateFactory factory;
    private final Template template;

    @Inject
    private HtmlExceptionWrapperBodyWriter() {
        this(new MustacheTemplateFactory());
    }

    protected HtmlExceptionWrapperBodyWriter(TemplateFactory factory) {
        super(ExceptionWrapper.class, TEXT_HTML_TYPE, UTF8);

        /* Access the input stream of our template */
        final InputStream input = type.getResourceAsStream(TEMPLATE_NAME);
        if (input == null) throw new IllegalStateException("Unable to locate " + TEMPLATE_NAME);

        /* Read the whole thing as a UTF-8 string */
        final Reader reader = new InputStreamReader(input, UTF8);
        final StringBuilder builder = new StringBuilder();
        final char[] buffer = new char[4096];
        try {
            int read;
            while ((read = reader.read(buffer)) >= 0)
                if (read > 0) builder.append(buffer, 0, read);
            reader.close();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + TEMPLATE_NAME, exception);
        }

        /* Create the factory and template */
        template = factory.parse(builder.toString());
        this.factory = factory;
    }

    @Override
    protected void writeTo(ExceptionWrapper instance, Annotation[] annotations, Writer writer)
    throws IOException, WebApplicationException {
        this.writeTo(instance, template, writer);
    }

    protected void writeTo(ExceptionWrapper instance, Template template, Writer writer)
    throws IOException, WebApplicationException {
        template.execute(writer, instance.compute(factory));
    }
}
