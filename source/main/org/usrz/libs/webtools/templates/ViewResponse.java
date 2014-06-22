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

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

import org.usrz.libs.webtools.templates.Scope.ScopeBuilder;

public class ViewResponse {

    private ViewResponse() {
        throw new IllegalStateException("Do not construct");
    }

    /* ====================================================================== */

    public static ScopedResponseBuilder fromResponse(Response response) {
        return new ScopedResponseBuilder(Response.fromResponse(response));
    }

    public static ScopedResponseBuilder status(StatusType status) {
        return new ScopedResponseBuilder(Response.status(status));
    }

    public static ScopedResponseBuilder status(Status status) {
        return new ScopedResponseBuilder(Response.status(status));
    }

    public static ScopedResponseBuilder status(int status) {
        return new ScopedResponseBuilder(Response.status(status));
    }

    public static ScopedResponseBuilder ok() {
        return new ScopedResponseBuilder(Response.ok());
    }

    public static ScopedResponseBuilder ok(Object entity) {
        return new ScopedResponseBuilder(Response.ok()).entity(entity);
    }

    public static ScopedResponseBuilder ok(Object entity, MediaType type) {
        return new ScopedResponseBuilder(Response.ok()).entity(entity).type(type);
    }

    public static ScopedResponseBuilder ok(Object entity, String type) {
        return new ScopedResponseBuilder(Response.ok()).entity(entity).type(type);
    }

    public static ScopedResponseBuilder ok(Object entity, Variant variant) {
        return new ScopedResponseBuilder(Response.ok()).entity(entity).variant(variant);
    }

    public static ScopedResponseBuilder serverError() {
        return new ScopedResponseBuilder(Response.serverError());
    }

    public static ScopedResponseBuilder created(URI location) {
        return new ScopedResponseBuilder(Response.created(location));
    }

    public static ScopedResponseBuilder accepted() {
        return new ScopedResponseBuilder(Response.accepted());
    }

    public static ScopedResponseBuilder accepted(Object entity) {
        return new ScopedResponseBuilder(Response.accepted()).entity(entity);
    }

    public static ScopedResponseBuilder noContent() {
        return new ScopedResponseBuilder(Response.noContent());
    }

    public static ScopedResponseBuilder notModified() {
        return new ScopedResponseBuilder(Response.notModified());
    }

    public static ScopedResponseBuilder notModified(EntityTag tag) {
        return new ScopedResponseBuilder(Response.notModified(tag));
    }

    public static ScopedResponseBuilder notModified(String tag) {
        return new ScopedResponseBuilder(Response.notModified(tag));
    }

    public static ScopedResponseBuilder seeOther(URI location) {
        return new ScopedResponseBuilder(Response.seeOther(location));
    }

    public static ScopedResponseBuilder temporaryRedirect(URI location) {
        return new ScopedResponseBuilder(Response.temporaryRedirect(location));
    }

    public static ScopedResponseBuilder notAcceptable(List<Variant> variants) {
        return new ScopedResponseBuilder(Response.notAcceptable(variants));
    }

    /* ====================================================================== */

    public static class ScopedResponseBuilder
    extends Response.ResponseBuilder
    implements Scoping<ScopedResponseBuilder> {

        private final ResponseBuilder response;

        private ScopeBuilder scope;
        private Object entity;

        private List<Annotation> annotations;
        private View view;

        private ScopedResponseBuilder(ResponseBuilder builder) {
            response = builder;
        }

        /* ====================================================================== */

        @Override
        public Response build() {
            final Object object = scope != null ? scope.build() : entity;

            final List<Annotation> list = new ArrayList<>();
            if (annotations != null) list.addAll(annotations);
            if (view != null) list.add(view);

            return (list.size() > 0 ?
                        response.entity(object, list.toArray(new Annotation[list.size()])) :
                        response.entity(object)
                   ).build();
        }

        /* ====================================================================== */

        public ScopedResponseBuilder view(String view) {
            return view(Views.view(view));
        }

        public ScopedResponseBuilder view(View view) {
            this.view = view;
            return this;
        }

        public ScopedResponseBuilder annotation(Annotation annotation) {
            if (annotation == null) annotations = null;
            else annotations = Collections.singletonList(annotation);
            return this;
        }

        public ScopedResponseBuilder annotations(Annotation... annotations) {
            if (annotations == null) this.annotations = null;
            else this.annotations = Arrays.asList(annotations);
            return this;
        }

        public ScopedResponseBuilder annotations(List<Annotation> annotations) {
            this.annotations = annotations;
            return this;
        }

        /* ====================================================================== */

        @Override
        public ScopedResponseBuilder with(String key, Object value) {
            if (entity != null) throw new IllegalStateException("Entity already set");
            if (scope == null) scope = Scope.builder();
            scope.with(key, value);
            return this;
        }

        @Override
        public ScopedResponseBuilder partial(String name, String partial) {
            if (entity != null) throw new IllegalStateException("Entity already set");
            if (scope == null) scope = Scope.builder();
            scope.partial(name, partial);
            return this;
        }

        @Override
        public ScopedResponseBuilder entity(Object entity) {
            if (scope != null) throw new IllegalStateException("Entity is a Scope");
            this.entity = entity;
            return this;
        }

        @Override
        public ScopedResponseBuilder entity(Object entity, Annotation[] annotations) {
            if (scope != null) throw new IllegalStateException("Entity is a Scope");
            this.annotations(annotations);
            this.entity = entity;
            return this;
        }

        /* ====================================================================== */

        @Override
        public ResponseBuilder clone() {
            return new ScopedResponseBuilder(response.clone());
        }

        /* ====================================================================== */

        @Override
        public ScopedResponseBuilder status(int status) {
            response.status(status);
            return this;
        }

        @Override
        public ScopedResponseBuilder status(StatusType status) {
            response.status(status);
            return this;
        }

        @Override
        public ScopedResponseBuilder status(Status status) {
            response.status(status);
            return this;
        }

        @Override
        public ScopedResponseBuilder allow(String... methods) {
            response.allow(methods);
            return this;
        }

        @Override
        public ScopedResponseBuilder allow(Set<String> methods) {
            response.allow(methods);
            return this;
        }

        @Override
        public ScopedResponseBuilder cacheControl(CacheControl cacheControl) {
            response.cacheControl(cacheControl);
            return this;
        }

        @Override
        public ScopedResponseBuilder encoding(String encoding) {
            response.encoding(encoding);
            return this;
        }

        @Override
        public ScopedResponseBuilder header(String name, Object value) {
            response.header(name, value);
            return this;
        }

        @Override
        public ScopedResponseBuilder replaceAll(MultivaluedMap<String, Object> headers) {
            response.replaceAll(headers);
            return this;
        }

        @Override
        public ScopedResponseBuilder language(String language) {
            response.language(language);
            return this;
        }

        @Override
        public ScopedResponseBuilder language(Locale language) {
            response.language(language);
            return this;
        }

        @Override
        public ScopedResponseBuilder type(MediaType type) {
            response.type(type);
            return this;
        }

        @Override
        public ScopedResponseBuilder type(String type) {
            response.type(type);
            return this;
        }

        @Override
        public ScopedResponseBuilder variant(Variant variant) {
            response.variant(variant);
            return this;
        }

        @Override
        public ScopedResponseBuilder contentLocation(URI location) {
            response.contentLocation(location);
            return this;
        }

        @Override
        public ScopedResponseBuilder cookie(NewCookie... cookies) {
            response.cookie(cookies);
            return this;
        }

        @Override
        public ScopedResponseBuilder expires(Date expires) {
            response.expires(expires);
            return this;
        }

        @Override
        public ScopedResponseBuilder lastModified(Date lastModified) {
            return this;
        }

        @Override
        public ScopedResponseBuilder location(URI location) {
            response.location(location);
            return this;
        }

        @Override
        public ScopedResponseBuilder tag(EntityTag tag) {
            response.tag(tag);
            return this;
        }

        @Override
        public ScopedResponseBuilder tag(String tag) {
            response.tag(tag);
            return this;
        }

        @Override
        public ScopedResponseBuilder variants(Variant... variants) {
            response.variants(variants);
            return this;
        }

        @Override
        public ScopedResponseBuilder variants(List<Variant> variants) {
            response.variants(variants);
            return this;
        }

        @Override
        public ScopedResponseBuilder links(Link... links) {
            response.links(links);
            return this;
        }

        @Override
        public ScopedResponseBuilder link(URI uri, String rel) {
            response.link(uri, rel);
            return this;
        }

        @Override
        public ScopedResponseBuilder link(String uri, String rel) {
            response.link(uri, rel);
            return this;
        }

    }
}
