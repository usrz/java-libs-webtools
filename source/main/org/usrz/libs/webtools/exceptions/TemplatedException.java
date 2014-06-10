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

import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static org.usrz.libs.utils.Check.notNull;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

public class TemplatedException extends RuntimeException {

    private final Map<String, String> partials;
    private final Map<String, Object> scope;
    private final StatusType status;

    private TemplatedException(String message,
                               StatusType status,
                               Map<String, String> partials,
                               Map<String, Object> scope) {
        super(message);
        this.status = status;
        this.scope = scope;
        this.partials = partials;
    }

    public StatusType getStatus() {
        return status;
    }

    public Map<String, Object> getScope() {
        return scope;
    }

    public Map<String, String> getPartials() {
        return partials;
    }

    @Override
    public TemplatedException initCause(Throwable cause) {
        super.initCause(cause);
        return this;
    }

    /* ====================================================================== */

    public static class Builder {

        private final Map<String, String> partials = new HashMap<>();
        private final Map<String, Object> scope = new HashMap<>();
        private StatusType status = Status.INTERNAL_SERVER_ERROR;
        private Throwable cause;
        private String message;

        public Builder() {
            /* Nothing to do */
        }

        public Builder message(String message) {
            scope.put("message", message);
            this.message = message;
            return this;
        }

        public Builder message(String format, Object... arguments) {
            return message(String.format(format, arguments));
        }

        public Builder cause(Throwable cause) {
            scope.put("cause", cause);
            this.cause = cause;
            return this;
        }

        public Builder status(int status) {
            return status(notNull(fromStatusCode(status), "Invalid status " + status));
        }

        public Builder status(Status status) {
            this.status = notNull(status, "Null status");
            return this;
        }

        public Builder status(StatusType status) {
            this.status = notNull(status, "Null status");
            return this;
        }

        public Builder partial(String key, String template) {
            partials.put(notNull(key, "Null key"), notNull(template, "Null template"));
            return this;
        }

        public Builder put(String key, Object value) {
            scope.put(notNull(key, "Null key"), notNull(value, "Null value"));
            return this;
        }

        public Builder putAll(Map<? extends String, ? extends Object> map) {
            scope.putAll(map);
            return this;
        }

        public TemplatedException build() {
            final TemplatedException exception = new TemplatedException(message,
                                                                        status,
                                                                        partials,
                                                                        scope);
            if (cause != null) exception.initCause(cause);
            return exception;
        }
    }
}
