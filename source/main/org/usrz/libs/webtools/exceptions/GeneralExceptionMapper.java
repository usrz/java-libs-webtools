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

import static org.usrz.libs.utils.Check.notNull;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.usrz.libs.logging.Log;

@Provider
@Singleton
@Priority(1000)
public class GeneralExceptionMapper implements ExceptionMapper<Throwable> {

    protected final Log log = new Log();
    protected final HttpHeaders headers;
    protected final UriInfo info;

    @Inject
    private GeneralExceptionMapper(@Context UriInfo info,
                                    @Context HttpHeaders headers) {
        this.headers = notNull(headers, "Null headers");
        this.info = notNull(info, "Null UriInfo");
    }

    @Override
    public Response toResponse(Throwable exception) {

        final ExceptionWrapper wrapper;

        if (exception == null) {
            wrapper = new ExceptionWrapper(new IllegalArgumentException("No exception to handle"));

        } else if (exception instanceof TemplatedException) {

            wrapper = new ExceptionWrapper(((TemplatedException) exception).getStatus(), exception);

        } else if (exception instanceof WebApplicationException) {

            /* Web application exceptions are complicated */
            final Response response = ((WebApplicationException) exception).getResponse();
            final Throwable cause = exception.getCause();

            /* Copy entity and status, no matter what */
            final StatusType status = response.getStatusInfo();

            switch (status.getFamily()) {
                case INFORMATIONAL: // 1xx
                case SUCCESSFUL:    // 2xx
                case REDIRECTION:   // 3xx
                case CLIENT_ERROR:  // 4xx

                    /* "Soft" exception? Only log the real cause if any */
                    wrapper = new ExceptionWrapper(status, cause);
                    break;

                case SERVER_ERROR:  // 5xx
                case OTHER:         // ???
                default:

                    /* "Hard" exception? Either use the real cause or the WAE */
                    wrapper = new ExceptionWrapper(status,  // status from response
                                                   cause != null ? cause : exception);
                    break;

            }
        } else {

            /* Any other exception is *always* a 500 */
            wrapper = new ExceptionWrapper(exception);

        }

        /* Log the exception *IF* we have to */
        final Throwable cause = wrapper.getException();
        if (cause != null)
            log.warn(cause, "Exception %s handling request for \"%s\" => %s",
                     wrapper.getReference(), info.getRequestUri(), wrapper.getMessage());

        /* Return our wrapper */
        return Response.status(wrapper.getStatus())
                       .entity(wrapper)
                       .build();
    }

}