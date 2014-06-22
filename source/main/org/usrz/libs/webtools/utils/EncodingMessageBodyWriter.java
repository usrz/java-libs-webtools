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
package org.usrz.libs.webtools.utils;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.CHARSET_PARAMETER;
import static org.usrz.libs.utils.Charsets.UTF8;
import static org.usrz.libs.utils.Check.notNull;

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

public abstract class EncodingMessageBodyWriter<T>
implements MessageBodyWriter<T> {

    protected final Class<T> type;
    protected final MediaType mediaType;
    private final Charset charset;

    /* ====================================================================== */

    protected EncodingMessageBodyWriter(Class<T> type, MediaType mediaType) {
        this(type, mediaType, null);
    }

    protected EncodingMessageBodyWriter(Class<T> type, MediaType mediaType, Charset charset) {

        /* Type is stored unchanged (and required) */
        this.type = notNull(type, "Null type");

        /* Try to figure out the charset from the MediaType */
        final String name = notNull(mediaType, "Null media type").getParameters().get(CHARSET_PARAMETER);
        this.charset = charset != null ? charset : // first, if, specified on constructor
                       name == null ? UTF8 : // UTF8 if not available in media type
                       Charset.forName(name); // whatever we have in media type

        /* Finally "normalize" the media type */
        this.mediaType = mediaType.withCharset(charset.name());
        if ((mediaType.isWildcardSubtype()) || (mediaType.isWildcardType()))
            throw new IllegalArgumentException("Invalid media type " + mediaType);
    }

    /* ====================================================================== */

    @Override @Deprecated
    public long getSize(T instance,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type,
                               Type genericType,
                               Annotation[] annotations,
                               MediaType mediaType) {
        return (this.type.isAssignableFrom(type))
            && (this.mediaType.isCompatible(mediaType));
    }

    @Override
    public void writeTo(T instance,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType responseMediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream)
    throws IOException, WebApplicationException {

        /* Content type if we have it */
        final MediaType actualMediaType;
        if (httpHeaders.containsKey(CONTENT_TYPE)) {
            actualMediaType = MediaType.valueOf(httpHeaders.getFirst(CONTENT_TYPE).toString());
        } else if (responseMediaType != null) {
            actualMediaType = responseMediaType;
        } else {
            actualMediaType = mediaType;
        }

        /* If we found a mediaType, then check if we have a charset */
        final Charset responseCharset;
        final String charsetName = actualMediaType.getParameters().get(CHARSET_PARAMETER);
        if (charsetName != null) {
            httpHeaders.putSingle(CONTENT_TYPE, actualMediaType);
            responseCharset = Charset.forName(charsetName);
        } else {
            httpHeaders.putSingle(CONTENT_TYPE, actualMediaType.withCharset(this.charset.name()));
            responseCharset = this.charset;
        }

        /* Write the response */
        final OutputStreamWriter entityWriter = new OutputStreamWriter(entityStream, responseCharset);
        this.writeTo(instance, annotations, entityWriter);
        entityWriter.flush();
        entityStream.flush();
    }

    protected abstract void writeTo(T instance,
                                    Annotation[] annotations,
                                    Writer entityWriter)
    throws IOException, WebApplicationException;

}
