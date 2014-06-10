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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"status_code", "status_reason", "reference", "message", "exception"})
public final class ExceptionWrapper {

    private final UUID uuid;
    private final StatusType status;
    private final String message;
    private final Throwable exception;

    protected ExceptionWrapper(Throwable exception) {
        this(Status.INTERNAL_SERVER_ERROR, exception);
    }

    protected ExceptionWrapper(StatusType status, Throwable exception) {
        this.status = notNull(status, "Null status");
        this.exception = exception;

        /* Message, either specified, or from exception, or from status */
        message = exception != null ?
                exception.getMessage() :
                status.getReasonPhrase();

        /* The reference UUID is only available if we have an exception */
        uuid = exception == null ? null : UUID.randomUUID();
    }

    @JsonIgnore
    public StatusType getStatus() {
        return status;
    }

    @JsonProperty("status_code")
    public int getStatusCode() {
        return status.getStatusCode();
    }

    @JsonProperty("status_reason")
    public String getStatusReason() {
        return status.getReasonPhrase();
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    @JsonProperty("reference")
    public UUID getReference() {
        return uuid;
    }

    @JsonIgnore
    public Throwable getException() {
        return exception;
    }

    @JsonProperty("exception")
    public Class<? extends Throwable> getExceptionType() {
        return exception == null ? null : exception.getClass();
    }

    public Map<String, Object> toMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put("status_code", getStatusCode());
        map.put("status_reason", getStatusReason());
        map.put("message", getMessage());
        map.put("reference", getReference());
        map.put("exception", getExceptionType());
        return map;
    }
}
