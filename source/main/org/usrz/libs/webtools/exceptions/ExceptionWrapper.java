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

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.UriInfo;

import org.usrz.libs.logging.Log;
import org.usrz.libs.webtools.mustache.MustacheTemplateFactory;
import org.usrz.libs.webtools.templates.Scope;
import org.usrz.libs.webtools.templates.TemplateFactory;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

@JsonSerialize(converter=ExceptionWrapper.Converter.class)
public class ExceptionWrapper implements Scope {

    /*
     * JSON conversion can happen by simply calling the "compute(...)" method.
     *
     * Package-private, as Eclipse seems to be able to compile it when used
     * in the @JsonSerialize annotation above, but the normal JDK compiler
     * seem to be having issues about it...
     */
    static class Converter extends StdConverter<ExceptionWrapper, Map<?, ?>>{
        private static final TemplateFactory factory = new MustacheTemplateFactory();

        @Override
        public Map<?, ?> convert(ExceptionWrapper value) {
            return value == null ? null : value.compute(factory);
        }
    };

    /* ====================================================================== */

    private static final Log log = new Log();

    private final StatusType status;
    private final Throwable cause;
    private final UUID reference;
    private final String message;
    private final String method;
    private final URI location;
    private final Scope scope;

    public ExceptionWrapper(Throwable wrapped, UriInfo uri, Request request) {
        if (wrapped == null) throw new NullPointerException("Null exception to wrap");

        if (wrapped instanceof WebApplicationException) {
            final WebApplicationException exception = (WebApplicationException) wrapped;
            final Response response = exception.getResponse();
            status = response.getStatusInfo();
            message = exception.getMessage();
            cause = exception.getCause();
        } else {
            message = "Exception caught processing request";
            status = INTERNAL_SERVER_ERROR;
            cause = wrapped;
        }

        scope = wrapped instanceof Scope ? (Scope) wrapped : null;
        reference = UUID.randomUUID();
        method = method(request);
        location = location(uri);
    }

    private final String method(Request request) {
        if (request != null) try {
            return request.getMethod();
        } catch (IllegalStateException exception) {
            log.warn(exception, "Unable to initalize request method for Exception Wrapper");
        }
        return null;
    }

    private final URI location(UriInfo uri) {
        if (uri != null) try {
            return uri.getRequestUri();
        } catch (IllegalStateException exception) {
            log.warn(exception, "Unable to initalize request location for Exception Wrapper");
        }
        return null;
    }

    public StatusType getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }

    public UUID getReference() {
        return reference;
    }

    public String getRequestMethod() {
        return method;
    }

    public URI getRequestURI() {
        return location;
    }

    @Override
    public Map<String, Object> compute(TemplateFactory templates) {
        final Map<String, Object> map = new HashMap<>();
        if (scope != null) map.putAll(scope.compute(templates));

        /* Status is always here, the other we normalize */
        map.put("status_code", status.getStatusCode());
        map.put("status_reason", status.getReasonPhrase());
        map.put("reference", reference);

        /* Exception cause */
        if (cause != null) {
            map.put("exception_type", cause.getClass().getName());
            map.put("exception_message", cause.getMessage());
        } else {
            map.remove("exception_type");
            map.remove("exception_message");
        }

        /* Message */
        if (message != null) {
            map.put("message", message);
        } else {
            map.remove("message");
        }

        /* Method */
        if (method != null) {
            map.put("request_method", method);
        } else {
            map.remove("request_method");
        }

        /* Location */
        if (location != null) {
            map.put("request_uri", location);
        } else {
            map.remove("request_uri");
        }

        /* Done */
        return map;
    }

}
