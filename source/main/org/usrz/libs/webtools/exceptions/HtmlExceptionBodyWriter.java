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

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static org.usrz.libs.utils.Charsets.UTF8;

import java.io.IOException;
import java.io.Writer;
import java.util.UUID;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;

import org.usrz.libs.webtools.AbstractMessageBodyWriter;

@Provider
@Singleton
@Priority(1001)
@Produces(TEXT_HTML)
public class HtmlExceptionBodyWriter extends AbstractMessageBodyWriter<ExceptionWrapper> {

    @Inject
    private HtmlExceptionBodyWriter() {
        super(ExceptionWrapper.class, TEXT_HTML_TYPE, UTF8);
    }

    @Override
    protected void writeTo(ExceptionWrapper wrapper, Writer entityWriter)
    throws IOException, WebApplicationException {
        entityWriter.append("<!DOCTYPE html>")
                    .append("<html>")
                      .append("<head>")
                        .append("<title>")
                        .append("Error ")
                        .append(Integer.toString(wrapper.getStatusCode()))
                        .append(": ")
                        .append(escapeHtml(wrapper.getStatusReason()))
                        .append("</title>")
                      .append("</head>")
                      .append("<body>")
                        .append("<h1>")
                          .append("Error ")
                          .append(Integer.toString(wrapper.getStatusCode()))
                          .append(": ")
                          .append(escapeHtml(wrapper.getStatusReason()))
                        .append("</h1>")
                        .append("<dl>");

        final String message = wrapper.getMessage();

        if (message != null)
            entityWriter.append("<dt>Message:</dt>")
                        .append("<dd>")
                          .append(escapeHtml(wrapper.getMessage()))
                        .append("</dd>");

        final UUID reference = wrapper.getReference();
        if (reference != null)
            entityWriter.append("<dt>Reference:</dt>")
                        .append("<dd>")
                          .append(reference.toString())
                        .append("</dd>");

        final Class<?> exception = wrapper.getExceptionType();
        if (exception != null)
            entityWriter.append("<dt>Exception:</dt>")
                        .append("<dd>")
                          .append(exception.getName())
                        .append("</dd>");

        entityWriter.append("</dl>")
                  .append("</body>")
                .append("</html>");
    }

    private static final String escapeHtml(CharSequence input) {
        final StringBuilder result = new StringBuilder();
        for (int x = 0; x < input.length(); x ++) {
            final char c = input.charAt(x);
            switch (c) {
                case '&':  result.append("&amp;"); break;
                case '"':  result.append("&quot;"); break;
                case '<':  result.append("&lt;"); break;
                case '>':  result.append("&gt;"); break;
                case '\'': result.append("&#x27;"); break;
                case '/':  result.append("&#x2f;"); break;
                default:
                    if (Character.isISOControl(c)) {
                        result.append("&#x")
                              .append(Integer.toHexString(c))
                              .append(';');
                    } else {
                        result.append(c);
                    }
            }
        }
        return result.toString();
    }
}
