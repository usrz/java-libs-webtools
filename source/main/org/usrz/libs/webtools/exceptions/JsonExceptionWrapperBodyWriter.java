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

import static com.fasterxml.jackson.databind.SerializationFeature.CLOSE_CLOSEABLE;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;
import static javax.ws.rs.Priorities.USER;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.usrz.libs.utils.Charsets.UTF8;
import static org.usrz.libs.utils.Check.notNull;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.Map;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;

import org.usrz.libs.webtools.mustache.MustacheTemplateFactory;
import org.usrz.libs.webtools.templates.TemplateFactory;
import org.usrz.libs.webtools.utils.EncodingMessageBodyWriter;

import com.fasterxml.jackson.databind.ObjectMapper;

@Provider
@Singleton
@Priority(USER + 1000)
@Produces(APPLICATION_JSON)
public class JsonExceptionWrapperBodyWriter extends EncodingMessageBodyWriter<ExceptionWrapper> {

    private final TemplateFactory factory;
    private final ObjectMapper mapper;

    @Inject
    private JsonExceptionWrapperBodyWriter(ObjectMapper mapper) {
        super(ExceptionWrapper.class, APPLICATION_JSON_TYPE, UTF8);
        this.mapper = notNull(mapper, "Null object mapper");

        /* Create the factory and template */
        factory = new MustacheTemplateFactory();
    }

    @Override
    protected void writeTo(ExceptionWrapper instance, Annotation[] annotations, Writer writer)
    throws IOException, WebApplicationException {
        final Map<String, Object> details = instance.compute(factory);
        writer.write(mapper.writer(ORDER_MAP_ENTRIES_BY_KEYS)
                           .without(CLOSE_CLOSEABLE)
                           .writeValueAsString(details));
    }

}
