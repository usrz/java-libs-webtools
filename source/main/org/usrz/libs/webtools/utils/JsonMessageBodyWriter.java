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
package org.usrz.libs.webtools.utils;

import static javax.ws.rs.Priorities.USER;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.usrz.libs.utils.Charsets.UTF8;
import static org.usrz.libs.utils.Check.notNull;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

@Provider
@Singleton
@Priority(USER)
@Produces(APPLICATION_JSON)
public class JsonMessageBodyWriter extends EncodingMessageBodyWriter<Object> {

    private final ObjectMapper mapper;

    @Inject
    private JsonMessageBodyWriter(ObjectMapper mapper) {
        super(Object.class, APPLICATION_JSON_TYPE, UTF8);
        this.mapper = notNull(mapper, "Null object mapper");
    }

    @Override
    protected void writeTo(Object instance, Annotation[] annotations, Writer writer)
    throws IOException, WebApplicationException {
        mapper.writeValue(new Writer() {

            @Override
            public void write(int c)
            throws IOException {
                writer.write(c);
            }

            @Override
            public void write(char[] buf, int off, int len)
            throws IOException {
                writer.write(buf, off, len);
            }

            @Override
            public void flush()
            throws IOException {
                writer.flush();
            }

            @Override
            public void close() {
                /* Avoid bug in Jackson always closing */
            }

        }, instance);
    }

}
