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

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.MediaType;

import org.usrz.libs.utils.Check;

/**
 * An utility class producing <em>JAX-RS</em> {@link MediaType}s from
 * {@link File} and <em>file names</em>.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
public class MediaTypes {

    private static final Map<String, MediaType> TYPES;

    static {
        final Properties properties = new Properties();
        try {
            final InputStream input = MediaTypes.class.getResourceAsStream("mimeTypes.properties");
            properties.load(input);
            input.close();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load mime types", exception);
        }

        final Map<String, MediaType> types = new HashMap<>();
        properties.forEach((extension, type) -> {
            final String mime = type.toString();
            final int slash = mime.indexOf('/');
            types.put(extension.toString(),
                      new SimpleMediaType(mime.substring(0, slash),
                                          mime.substring(slash + 1),
                                          null));
        });

        TYPES = Collections.unmodifiableMap(types);
    }

    /* ====================================================================== */

    private MediaTypes() {
        throw new IllegalStateException("Do not construct");
    }

    /**
     * Return the {@link MediaType} associated with the specified {@link File}.
     * <p>
     * This is equivalent to {@link #get(String) get(file.getName()}.
     *
     * @return A <b>not-null</b> {@link MediaType} describing the specified
     *         {@link File} type (or <em>application/octet-stream</em> if
     *         the actual type was not known).
     */
    public static MediaType get(File file) {
        return get(Check.notNull(file, "Null file").getName());
    }

    /**
     * Return the {@link MediaType} associated with the specified file name.
     * <p>
     * File names can be fully specified {@code name.ext}, only dot-prefixed
     * extensions {@code .ext} or simple extensions {@code ext}.
     *
     * @return A <b>not-null</b> {@link MediaType} describing the specified
     *         file name type (or <em>application/octet-stream</em> if
     *         the actual type was not known).
     */
    public static MediaType get(String fileName) {
        if (fileName == null) return null;
        final String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        final MediaType type = TYPES.get(extension);
        return type != null ? type : APPLICATION_OCTET_STREAM_TYPE;
    }

    /* ====================================================================== */

    private static final class SimpleMediaType extends MediaType {

        private SimpleMediaType(String type, String subType, String charset) {
            super(type, subType, charset);
        }

        @Override
        public SimpleMediaType withCharset(String charset) {
            return new SimpleMediaType(getType(), getSubtype(), charset);
        }

        @Override
        public String toString() {
            try {
                /* When we do have a RuntimeDelegate */
                return super.toString();
            } catch (Exception exception) {
                /* When we don't have a RuntimeDelegate */
                final StringBuilder builder = new StringBuilder(getType()).append('/').append(getSubtype());
                getParameters().forEach((k, v) -> builder.append("; ").append(k).append('=').append(v));
                return builder.toString();
            }
        }


    }
}
