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

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.usrz.libs.webtools.exceptions.AbstractExceptionMapper;

@Provider
@Singleton
@Priority(1001)
public class TemplatedExceptionMapper
extends AbstractExceptionMapper<TemplatedException> {

    private final TemplateFactory templates;

    @Inject
    protected TemplatedExceptionMapper(TemplateFactory templates,
                                       @Context UriInfo info,
                                       @Context HttpHeaders headers) {
        super(info, headers);
        this.templates = notNull(templates, "Null template factory");
    }

    @Override
    public Response toResponse(TemplatedException exception) {
        return super.toResponse(exception.renderPartials(templates));
    }

}