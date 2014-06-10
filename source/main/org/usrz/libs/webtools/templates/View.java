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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.usrz.libs.utils.Check;

public final class View {

    final String template;
    final Object scope;

    public View(String template) {
        this(template, null);
    }

    public View(String template, Object scope) {
        this.template = Check.notNull(template, "Null template name");
        this.scope = scope == null ? Collections.EMPTY_MAP : scope;
    }

    public static final Builder template(String template) {
        return new Builder(template);
    }

    public static final class Builder {

        private final String template;
        private final Map<String, Object> scope = new HashMap<>();

        private Builder(String template) {
            this.template = Check.notNull(template, "Null template name");
        }

        public Builder with(String key, Object value) {
            scope.put(key, value);
            return this;
        }

        public View view() {
            return new View(template, scope);
        }

        public ResponseBuilder response() {
            return Response.ok(view());
        }

        public ResponseBuilder response(int status) {
            return Response.status(status).entity(view());
        }

        public ResponseBuilder response(Response.Status status) {
            return Response.status(status).entity(view());
        }

        public ResponseBuilder response(Response.StatusType status) {
            return Response.status(status).entity(view());
        }

    }

}
