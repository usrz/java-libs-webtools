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

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static org.usrz.libs.utils.Charsets.UTF8;
import static org.usrz.libs.utils.Check.notNull;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.ext.Provider;

import org.usrz.libs.logging.Log;
import org.usrz.libs.webtools.AbstractMessageBodyWriter;
import org.usrz.libs.webtools.exceptions.ExceptionWrapper;

@Provider
@Singleton
@Priority(1002)
@Produces(TEXT_HTML)
public class TemplateExceptionBodyWriter extends AbstractMessageBodyWriter<ExceptionWrapper> {

    private final Log log = new Log();
    private final TemplateFactory templates;
    private final CompiledTemplate template;

    @Inject
    private TemplateExceptionBodyWriter(TemplateFactory templates) {
        super(ExceptionWrapper.class, TEXT_HTML_TYPE, UTF8);
        this.templates = notNull(templates, "Null template factory");
        template = templates.compileInline(HTML);
    }

    private CompiledTemplate findTemplate(StatusType status) {
        final String byCode = "errors/" + Integer.toString(status.getStatusCode());
        if (templates.canCompile(byCode)) return templates.compile(byCode);

        final String byFamily;
        switch (status.getFamily()) {
            case INFORMATIONAL: byFamily = "errors/1xx"; break; // 1xx
            case SUCCESSFUL:    byFamily = "errors/2xx"; break; // 2xx
            case REDIRECTION:   byFamily = "errors/3xx"; break; // 3xx
            case CLIENT_ERROR:  byFamily = "errors/4xx"; break; // 4xx
            case SERVER_ERROR:  byFamily = "errors/5xx"; break; // 5xx
            default:            byFamily = null; break; // ???
        }
        if ((byFamily != null) && (templates.canCompile(byFamily)))
            return templates.compile(byFamily);

        if (templates.canCompile("errors/error"))
            return templates.compile("errors/error");

        if (templates.canCompile("error"))
            return templates.compile("error");

        log.info("No template found for HTTP/%s, returning default", status.getStatusCode());
        return template;
    }

    @Override
    protected void writeTo(ExceptionWrapper wrapper, Writer entityWriter)
    throws IOException, WebApplicationException {
        findTemplate(wrapper.getStatus()).execute(entityWriter, wrapper);
    }

    private final String HTML = "<!DOCTYPE html>"
                              + "<html>"
                              + "<head>"
                              + "<title>Error {{statusCode}}: {{statusReason}}</title>"
                              + "</head>"
                              + "<body>"
                              + "<h1>Error {{statusCode}}: {{statusReason}}</h1>"
                              + "<dl>"
                              + "{{#message}}<dt>Message:</dt>"
                              + "<dd>{{message}}</dd>{{/message}}"
                              + "{{#reference}}<dt>Reference:</dt>"
                              + "<dd>{{reference}}</dd>{{/reference}}"
                              + "{{#exceptionType}}<dt>Exception:</dt>"
                              + "<dd>{{exceptionType}}</dd>{{/exceptionType}}"
                              + "</dl>"
                              + "</body>"
                              + "</html>";
}
