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
package org.usrz.libs.webtools.validation;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.usrz.libs.logging.Log;
import org.usrz.libs.webtools.exceptions.WebApplicationExceptionBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;



@Validate
@Provider
@Singleton
public class ValidationInterceptor implements ReaderInterceptor {

    private final Log log = new Log();
    private final Validator validator;
    private final PropertyNamingStrategy naming;
    private final String withName;

    @Inject
    private ValidationInterceptor(ObjectMapper objectMapper) {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        log.info("Validation interceptor using validator %s", validator);
        naming = objectMapper.getSerializationConfig().getPropertyNamingStrategy();
        withName = nameFor("constraintViolations");
    }

    private String nameFor(String name) {
        try {
            return naming.nameForField(null, null, name);
        } catch (Exception exception) {
            return name;
        }
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context)
    throws IOException, WebApplicationException {
        final Object object = context.proceed();
        if (object == null) return null;

        log.debug("Validating class %s", object.getClass().getName());
        final Set<ConstraintViolation<Object>> violations = validator.validate(object);
        if ((violations == null) || (violations.isEmpty())) return object;

        final List<Error> errors = new ArrayList<>();
        violations.forEach((violation) -> errors.add(new Error(violation)));

        throw new WebApplicationExceptionBuilder(BAD_REQUEST)
                .message("Violation constraint in API")
                .with(withName, errors)
                .build();
    }

    private final class Error {

        private final String message;
        private final String path;

        private Error(ConstraintViolation<?> constraint) {
            if (constraint == null) {
                message = "Null constraint violation";
                path = "";
            } else {
                final String message = constraint.getMessage();
                this.message = message == null ? "Unknown validation error" : message;

                final Path path = constraint.getPropertyPath();
                final StringBuilder pathBuilder = new StringBuilder();
                path.forEach((node) -> pathBuilder.append('.').append(nameFor(node.getName())));
                this.path = pathBuilder.length() == 0 ? "" : pathBuilder.substring(1);
            }
        }

        @JsonProperty
        public String getMessage() {
            return message;
        }

        @JsonProperty
        public String getPath() {
            return path;
        }
    }

}
