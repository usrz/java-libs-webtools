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

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.Variant;

import org.usrz.libs.webtools.templates.Scope;
import org.usrz.libs.webtools.templates.Scope.ScopeBuilder;
import org.usrz.libs.webtools.templates.Scoping;
import org.usrz.libs.webtools.templates.TemplateFactory;

public class WebApplicationExceptionBuilder
implements Scoping<WebApplicationExceptionBuilder> {

    private final ResponseBuilder responseBuilder;
    private final ScopeBuilder scopeBuilder;
    private Throwable cause;
    private String message;

    /* ====================================================================== */

    public WebApplicationExceptionBuilder() {
        this(INTERNAL_SERVER_ERROR);
    }

    public WebApplicationExceptionBuilder(int status) {
        this(Status.fromStatusCode(status));
    }

    public WebApplicationExceptionBuilder(Status status) {
        this((StatusType) status);
    }

    public WebApplicationExceptionBuilder(StatusType status) {
        responseBuilder = Response.status(status);
        scopeBuilder = Scope.builder();
    }

    public WebApplicationExceptionBuilder(String message) {
        this();
        this.message = message;
    }

    public WebApplicationExceptionBuilder(String format, Object... arguments) {
        this(String.format(format, arguments));
    }

    /* ====================================================================== */

    public WebApplicationException build() {
        final Response response = responseBuilder.build();
        final Scope scope = scopeBuilder.build();
        final WebApplicationException exception = new ScopedWebApplicationException(response, scope, message);
        if (cause != null) exception.initCause(cause);
        return exception;
    }

    /* ====================================================================== */

    @Override
    public WebApplicationExceptionBuilder with(String key, Object value) {
        scopeBuilder.with(key, value);
        return this;
    }

    @Override
    public WebApplicationExceptionBuilder partial(String name, String partial) {
        scopeBuilder.partial(name, partial);
        return this;
    }

    public WebApplicationExceptionBuilder entity(Object entity) {
        responseBuilder.entity(entity);
        return this;
    }

    public WebApplicationExceptionBuilder entity(Object entity, Annotation[] annotations) {
        responseBuilder.entity(entity, annotations);
        return this;
    }

    /* ====================================================================== */

    public final WebApplicationExceptionBuilder cause(Throwable cause) {
        this.cause = cause;
        return this;
    }

    public final WebApplicationExceptionBuilder message(String message) {
        this.message = message;
        return this;
    }

    public final WebApplicationExceptionBuilder message(String format, Object... arguments) {
        return this.message(String.format(format, arguments));
    }

    /* ====================================================================== */

    public final WebApplicationExceptionBuilder status(int status) {
        return this.status(Status.fromStatusCode(status));
    }

    public final WebApplicationExceptionBuilder status(Status status) {
        return this.status((StatusType) status);
    }

    public final WebApplicationExceptionBuilder status(StatusType status) {
        responseBuilder.status(status);
        return this;
    }

    /* ====================================================================== */

    public final WebApplicationExceptionBuilder allow(String... methods) {
        responseBuilder.allow(methods);
        return this;
    }

    public final WebApplicationExceptionBuilder allow(Set<String> methods) {
        responseBuilder.allow(methods);
        return this;
    }

    public final WebApplicationExceptionBuilder cacheControl(CacheControl cacheControl) {
        responseBuilder.cacheControl(cacheControl);
        return this;
    }

    public final WebApplicationExceptionBuilder encoding(String encoding) {
        responseBuilder.encoding(encoding);
        return this;
    }

    public final WebApplicationExceptionBuilder header(String name, Object value) {
        responseBuilder.header(name, value);
        return this;
    }

    public final WebApplicationExceptionBuilder replaceAll(MultivaluedMap<String, Object> headers) {
        responseBuilder.replaceAll(headers);
        return this;
    }

    public final WebApplicationExceptionBuilder language(String language) {
        responseBuilder.language(language);
        return this;
    }

    public final WebApplicationExceptionBuilder language(Locale language) {
        responseBuilder.language(language);
        return this;
    }

    public final WebApplicationExceptionBuilder type(MediaType type) {
        responseBuilder.type(type);
        return this;
    }

    public final WebApplicationExceptionBuilder type(String type) {
        responseBuilder.type(type);
        return this;
    }

    public final WebApplicationExceptionBuilder variant(Variant variant) {
        responseBuilder.variant(variant);
        return this;
    }

    public final WebApplicationExceptionBuilder contentLocation(URI location) {
        responseBuilder.contentLocation(location);
        return this;
    }

    public final WebApplicationExceptionBuilder cookie(NewCookie... cookies) {
        responseBuilder.cookie(cookies);
        return this;
    }

    public final WebApplicationExceptionBuilder expires(Date expires) {
        responseBuilder.expires(expires);
        return this;
    }

    public final WebApplicationExceptionBuilder lastModified(Date lastModified) {
        responseBuilder.lastModified(lastModified);
        return this;
    }

    public final WebApplicationExceptionBuilder location(URI location) {
        responseBuilder.location(location);
        return this;
    }

    public final WebApplicationExceptionBuilder tag(EntityTag tag) {
        responseBuilder.tag(tag);
        return this;
    }

    public final WebApplicationExceptionBuilder tag(String tag) {
        responseBuilder.tag(tag);
        return this;
    }

    public final WebApplicationExceptionBuilder variants(Variant... variants) {
        responseBuilder.variants(variants);
        return this;
    }

    public final WebApplicationExceptionBuilder variants(List<Variant> variants) {
        responseBuilder.variants(variants);
        return this;
    }

    public final WebApplicationExceptionBuilder links(Link... links) {
        responseBuilder.links(links);
        return this;
    }

    public final WebApplicationExceptionBuilder link(URI uri, String rel) {
        responseBuilder.link(uri, rel);
        return this;
    }

    public final WebApplicationExceptionBuilder link(String uri, String rel) {
        responseBuilder.link(uri, rel);
        return this;
    }

    /* ====================================================================== */

    private final class ScopedWebApplicationException
    extends WebApplicationException
    implements Scope {

        private final Scope scope;

        private ScopedWebApplicationException(Response response,
                                              Scope scope,
                                              String message) {
            super(message, response);
            this.scope = scope;
        }

        @Override
        public Map<String, Object> compute(TemplateFactory templates) {
            return scope.compute(templates);
        }
    }
}
