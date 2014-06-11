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

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.CHARSET_PARAMETER;
import static org.usrz.libs.utils.Charsets.UTF8;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.usrz.libs.webtools.AbstractMessageBodyWriter;

@Provider
@Singleton
public class TemplateBodyWriter extends AbstractMessageBodyWriter<Object> {

    private static final String UTF8_NAME = UTF8.name();
    private final TemplateFactory factory;

    @Inject
    public TemplateBodyWriter(TemplateFactory factory) {
        this.factory = factory;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (View.class.isAssignableFrom(type)) return true;
        return template(annotations) != null;
    }

    @Override
    public void writeTo(Object object, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream stream)
    throws IOException, WebApplicationException {

        /* Figure out scopes and templates */
        final Object scopes;
        final String template;

        if (object instanceof View) {
            final View view = (View) object;
            scopes = view.scope;
            template = view.template;
        } else {
            scopes = object;
            template = template(annotations);
        }

        /* Check charset, default to UTF8 */
        String charset = mediaType.getParameters().get(CHARSET_PARAMETER);
        if (charset == null) {
            charset = UTF8_NAME;
            mediaType = mediaType.withCharset(UTF8_NAME);
        }

        /* Override mime type header */
        httpHeaders.putSingle(CONTENT_TYPE, mediaType.toString());

        /* Create writer and merge our template */
        final Writer writer = new OutputStreamWriter(stream, charset);
        factory.compile(template).execute(writer, scopes);
        writer.flush();
        stream.flush();
    }

    /* ====================================================================== */

    private final String template(Annotation[] annotations) {
        if (annotations == null) return null;
        for (Annotation annotation: annotations) {
            if (annotation.annotationType().equals(Template.class)) {
                return ((Template) annotation).value();
            }
        }
        return null;
    }
}
