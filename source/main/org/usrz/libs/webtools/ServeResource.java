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

import static org.usrz.libs.utils.Charsets.UTF8;
import static org.usrz.libs.utils.Check.notNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.logging.Log;

/**
 * A <em>JAX-RS</em> resource serving static files.
 * <p>
 * This resource will automatically convert <em>{@linkplain LessCSS}</em> files
 * into <em>CSS</em>, and optionally <em>minify</em> JavaScript and CSS files.
 * <p>
 * All processed content (Less sources, minified and JavaScript and CSS) will
 * be cached in memory and reprocessed only if the source file changes and
 * cache headers will be produced.
 * <p>
 * Configurations are as follows:
 * <dl>
 *   <dt>{@code root_path}</dt>
 *   <dd><em>(Required)</em> The path containing all the resources to serve.
 *   <dt>{@code minify}</dt>
 *   <dd><em>(Default: {@code false})</em> Whether to minify JavaScript, CSS and LessCSS resources.</dd>
 *   <dt>{@code cache}</dt>
 *   <dd><em>(Default: {@code no-cache})</em> A {@link Duration} for the HTTP cache headers.</dd>
 * </dl>
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
@Path("/")
@Singleton
public class ServeResource {

    private static final MediaType MEDIA_TYPE_CSS = new MediaType("text", "css").withCharset(UTF8.name());
    private static final MediaType MEDIA_TYPE_JS = new MediaType("application", "javascript").withCharset(UTF8.name());

    private static final Log log = new Log();

    private final LessCSS less = new LessCSS();
    private final UglifyJS2 uglify = new UglifyJS2();
    private final ConcurrentMap<File, Entry> cache = new ConcurrentHashMap<>();

    private final File root;
    private final boolean minify;
    private final Duration cacheDuration;
    private final CacheControl cacheControl;

    /**
     * Create a new {@link ServeResource} instance with the specified
     * {@link Configurations}.
     */
    @Inject
    public ServeResource(Configurations configurations)
    throws IOException {
        root = configurations.requireFile("root_path").getCanonicalFile();

        if (! root.isDirectory())
            throw new IllegalArgumentException("Invalid resource root \"" + root + "\"");
        minify = configurations.get("minify", false);

        cacheDuration = configurations.getDuration("cache", Duration.ZERO);
        cacheControl = new CacheControl();
        cacheControl.setMaxAge((int) cacheDuration.getSeconds());
        cacheControl.setNoCache(Duration.ZERO.equals(cacheDuration));
    }

    /**
     * Serve the specified resource (either {@code GET} or {@code HEAD}).
     */
    @GET
    @Path("{resource:.*}")
    public Response serve(@PathParam("resource") String path)
    throws IOException {

        /* Basic check for null/empty path */
        if ((path == null) || (path.length() == 0)) throw new NotFoundException();

        /* Get our resource file, potentially a ".less" file for CSS */
        File resourceFile = new File(root, path);
        if (! resourceFile.isFile() && path.endsWith(".css")) {
            resourceFile = new File(root, path.substring(0, path.length() - 4) + ".less");
        }

        /* Check if the file is in our resource directory (fake requests and whatnot) */
        File parentFile = resourceFile.getParentFile();
        while (parentFile != null) {
            if (parentFile.equals(root)) break;
            parentFile = parentFile.getParentFile();

        }

        /* If the root is incorrect, log this, if not found, 404 it! */
        if ((parentFile == null) || (! root.equals(parentFile))) {
            log.warn("Attempted to access resource outside of root \"%s\"", path);
            throw new NotFoundException();
        } else if (! resourceFile.isFile()) {
            throw new NotFoundException();
        }

        /* Check and validated our cache */
        final String fileName = resourceFile.getName();
        Entry cached = cache.computeIfPresent(resourceFile, (file, entry) ->
            file.lastModified() == entry.lastModified ? entry : null);

        /* If we have no cache, we *might* want to cache something */
        if (cached == null) {

            /* What to do, what to do? */
            if ((fileName.endsWith(".css") && minify) || fileName.endsWith(".less")) {

                /* Lessify CSS and cache */
                log.debug("Lessifying resource \"%s\"", resourceFile);
                cached = new Entry(resourceFile.lastModified(),
                                   less.convert(load(resourceFile), minify),
                                   MEDIA_TYPE_CSS);

            } else if (fileName.endsWith(".js") && minify) {

                /* Uglify JavaScript and cache */
                log.debug("Uglifying resource \"%s\"", resourceFile);
                cached = new Entry(resourceFile.lastModified(),
                                   uglify.convert(load(resourceFile), minify, minify),
                                   MEDIA_TYPE_JS);
            }

            /* Do we have anything to cache? */
            if (cached != null) {
                log.debug("Caching resource \"%s\"", resourceFile);
                cache.put(resourceFile, cached);
            }
        }

        /* Prepare our basic response from either cache or file */
        final ResponseBuilder response = Response.ok();
        if (cached != null) {
            log.debug("Serving cached resource \"%s\"", resourceFile);
            response.entity(cached.contents)
                    .lastModified(new Date(cached.lastModified))
                    .type(cached.type);

        } else {
            log.debug("Serving file resource \"%s\"", resourceFile);
            response.entity(resourceFile)
                    .lastModified(new Date(resourceFile.lastModified()))
                    .type(MediaTypes.get(resourceFile));
        }

        /* Caching headers and build response */
        final Date expires = Date.from(Instant.now().plus(cacheDuration));
        return response.cacheControl(cacheControl)
                       .expires(expires)
                       .build();
    }

    /* ====================================================================== */

    private final String load(File file)
    throws IOException {
        final FileInputStream input = new FileInputStream(file);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final byte[] buffer = new byte[65536];
        int read = -1;
        while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
        input.close();
        output.close();
        return new String(output.toByteArray(), UTF8);
    }

    /* ====================================================================== */

    private final class Entry {

        private final long lastModified;
        private final String contents;
        private final MediaType type;

        private Entry(long lastModified, String contents, MediaType type) {
            this.lastModified = lastModified;
            this.contents = notNull(contents);
            this.type = notNull(type);
        }
    }
}
