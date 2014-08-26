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
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.usrz.libs.utils.Check.notNull;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.usrz.libs.logging.Log;

@Provider
@Singleton
@Priority(USER)
public class ExceptionWrapperMapper implements ExceptionMapper<Throwable> {

    private static final String LOG_FORMAT = "%s - %s %s - HTTP/%d %s - \"%s\"";
    private final Log log = new Log("exceptions");
    private final Request request;
    private final UriInfo uri;

    @Inject
    private ExceptionWrapperMapper(@Context Request request,
                                   @Context UriInfo uri) {
        this.request = notNull(request, "Null Request info");
        this.uri = notNull(uri, "Null URI info");
    }

    private ExceptionWrapper wrapAndLog(Throwable throwable) {

        final ExceptionWrapper wrapper = new ExceptionWrapper(throwable, uri, request);

        final StatusType status = wrapper.getStatus();
        final int statusCode = status.getStatusCode();

        final Object[] arguments = new Object[] {
                wrapper.getReference(),
                wrapper.getRequestMethod(),
                wrapper.getRequestURI(),
                status.getStatusCode(),
                status.getReasonPhrase(),
                wrapper.getMessage() };

        if (statusCode < 400) {
            log.info(LOG_FORMAT, arguments); /* Just in case */
        } else if (statusCode < 500) {
            log.warn(throwable, LOG_FORMAT, arguments);
        } else {
            log.error(throwable, LOG_FORMAT, arguments);
        }

        return wrapper;
    }

    @Override
    public Response toResponse(Throwable throwable) {

        /* If this is a "WebApplicationException" we handle it in a special way */
        if (throwable instanceof WebApplicationException) {

            /*
             * Remember, this only gets called when the exception's entity is
             * "null", otherwise JAX-RS will simply build the request on its
             * own and invoke the appropriate MessageBodyWriter...
             */
            final WebApplicationException exception = (WebApplicationException) throwable;

            /* We don't really care about anything that is not an error. */
            final Response response = exception.getResponse();
            if (response.getStatus() < 400) return response;

            /* Return a *NEW* response, with the ExceptionWrapper as entity */
            System.err.println("WRAPPER FROM " + exception);
            return Response.fromResponse(response)
                           .entity(wrapAndLog(exception))
                           .build();
        }

        /* This is not a "WebApplicationException", just wrap it! */
        return Response.status(INTERNAL_SERVER_ERROR)
                       .entity(wrapAndLog(throwable))
                       .build();

    }

}
