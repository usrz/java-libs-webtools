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
package org.usrz.libs.webtools;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.CHARSET_PARAMETER;
import static org.usrz.libs.utils.Charsets.UTF8;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.usrz.libs.utils.Charsets;

public abstract class AbstractMessageBodyWriter<T>
implements MessageBodyWriter<T> {

    protected final Class<T> type;
    protected final MediaType mediaType;
    private final Charset charset;

    protected AbstractMessageBodyWriter() {
        this(null, null);
    }

    protected AbstractMessageBodyWriter(Class<T> type) {
        this(type, null);
    }

    protected AbstractMessageBodyWriter(MediaType mediaType) {
        this(null, mediaType);
    }

    protected AbstractMessageBodyWriter(Class<T> type, MediaType mediaType) {
        this.type = type;

        if (mediaType != null) {
            final String charset = mediaType.getParameters().get(CHARSET_PARAMETER);
            this.charset = charset == null ? UTF8 : Charset.forName(charset);
            this.mediaType = mediaType.withCharset(this.charset.name());
        } else {
            this.mediaType = null;
            this.charset = Charsets.UTF8;
        }
    }

    protected AbstractMessageBodyWriter(Class<T> type, MediaType mediaType, Charset charset) {
        this.type = type;
        this.mediaType = mediaType == null ? null :
                         charset == null ? mediaType :
                         mediaType.withCharset(charset.name());
        this.charset = charset == null ? UTF8 : charset;
    }

    @Override
    public boolean isWriteable(Class<?> type,
                               Type genericType,
                               Annotation[] annotations,
                               MediaType mediaType) {
        /*
         * The order on mediaTypes is important. We only want to match if we
         * have been given a media type for this request, otherwise let
         * Jersey negotiate whatever... See @Priority at the top!
         */
        return (this.type == null ? true : this.type.isAssignableFrom(type))
            && (mediaType == null ? true : mediaType.isCompatible(this.mediaType));
    }

    @Override @Deprecated
    public long getSize(T instance,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(T instance,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream)
    throws IOException, WebApplicationException {

        System.err.println("MEDIATYPE-->" + httpHeaders.get(CONTENT_TYPE));
        System.err.println("         -->" + mediaType);
        System.err.println("         -->" + this.mediaType);

        /* Content type if we have it */
        final MediaType actualMediaType;
        if (httpHeaders.containsKey(CONTENT_TYPE)) {
            actualMediaType = MediaType.valueOf(httpHeaders.getFirst(CONTENT_TYPE).toString());
        } else if (mediaType != null) {
            actualMediaType = mediaType;
        } else {
            actualMediaType = this.mediaType;
        }

        /* If we found a mediaType, then check if we have a charset */
        if (actualMediaType != null) {
            final String charset = actualMediaType.getParameters().get(CHARSET_PARAMETER);
            if (charset != null) {
                httpHeaders.putSingle(CONTENT_TYPE, actualMediaType);
            } else {
                httpHeaders.putSingle(CONTENT_TYPE, actualMediaType.withCharset(this.charset.name()));
            }
        }

        /* Write the response */
        this.writeTo(instance, entityStream);
        entityStream.flush();
    }

    protected void writeTo(T instance, OutputStream entityStream)
    throws IOException, WebApplicationException {
        final OutputStreamWriter writer = new OutputStreamWriter(entityStream, charset);
        this.writeTo(instance, writer);
        writer.flush();
    }

    protected void writeTo(T instance, Writer entityWriter)
    throws IOException, WebApplicationException {
        throw new UnsupportedOperationException("Method \"writeTo(...)\" not implemented in " + this.getClass().getName());
    }
}
