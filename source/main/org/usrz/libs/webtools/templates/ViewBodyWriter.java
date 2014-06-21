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

import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static org.usrz.libs.utils.Charsets.UTF8;
import static org.usrz.libs.utils.Check.notNull;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.usrz.libs.webtools.utils.EncodingMessageBodyWriter;

@Provider
@Singleton
public class ViewBodyWriter extends EncodingMessageBodyWriter<Object> {

    private final TemplateManager manager;

    @Inject
    private ViewBodyWriter(TemplateManager manager) {
        super(Object.class, TEXT_HTML_TYPE, UTF8);
        this.manager = notNull(manager, "Null template manager");
    }

    @Override
    public boolean isWriteable(Class<?> type,
                               Type genericType,
                               Annotation[] annotations,
                               MediaType mediaType) {
        if (super.isWriteable(type, genericType, annotations, mediaType))
            for (Annotation annotation: annotations)
                if (View.class.isAssignableFrom(annotation.annotationType()))
                    return true;
        return false;
    }

    @Override
    public void writeTo(Object object, Annotation[] annotations, Writer writer)
    throws IOException, WebApplicationException {

        System.err.println("SCOPE IS WRITING -> " + object);

        /* Figure our the view: the last one takes precedence */
        View view = null;
        for (Annotation annotation: annotations)
            if (View.class.isAssignableFrom(annotation.annotationType()))
                view = (View) annotation;
        if (view == null)
            throw new WebApplicationException("No view for response");

        /* Compute the scope if necessary... */
        if (object instanceof Scope) object = ((Scope) object).compute(manager);

        /* Figure out scopes and templates */
        try {
            manager.compile(view.value()).execute(writer, object);
        } catch (TemplateException exception) {
            throw new WebApplicationException("Error rendering templates", exception);
        } catch (IOException | WebApplicationException exception) {
            throw exception;
        }
    }
}
