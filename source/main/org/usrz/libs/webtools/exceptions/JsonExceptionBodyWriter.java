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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.usrz.libs.utils.Charsets.UTF8;
import static org.usrz.libs.utils.Check.notNull;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;

import org.usrz.libs.webtools.AbstractMessageBodyWriter;

import com.fasterxml.jackson.databind.ObjectMapper;

@Provider
@Singleton
@Priority(1000)
@Produces(APPLICATION_JSON)
public class JsonExceptionBodyWriter extends AbstractMessageBodyWriter<ExceptionWrapper> {

    private final ObjectMapper mapper;

    @Inject
    private JsonExceptionBodyWriter(ObjectMapper mapper) {
        super(ExceptionWrapper.class, APPLICATION_JSON_TYPE, UTF8);
        this.mapper = notNull(mapper, "Null object mapper");
    }

    @Override
    protected void writeTo(ExceptionWrapper wrapper, Writer writer)
    throws IOException, WebApplicationException {
        /* Write a *STRING* since no matter what, Jackson *closes* the writer */
        writer.write(mapper.writeValueAsString(wrapper));
    }


}
