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
package org.usrz.libs.webtools.resources;

import static org.usrz.libs.utils.Charsets.UTF8;
import static org.usrz.libs.utils.Check.notNull;

import java.io.IOException;
import java.nio.charset.Charset;
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
import org.usrz.libs.webtools.lesscss.LessCSS;
import org.usrz.libs.webtools.uglifyjs.UglifyJS;
import org.usrz.libs.webtools.utils.MediaTypes;

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
 *   <dt>{@code charset}</dt>
 *   <dd><em>(Default: {@code UTF-8})</em> The default charset name for text files.</dd>
 * </dl>
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 */
@Path("/")
@Singleton
public class ServeResource {

    private final MediaType styleMediaType;
    private final MediaType scriptMediaType;

    private static final Log log = new Log();

    private final LessCSS less = new LessCSS();
    private final UglifyJS uglify = new UglifyJS();
    private final ConcurrentMap<Resource, Entry> cache = new ConcurrentHashMap<>();

    private final ResourceManager manager;
    private final boolean minify;
    private final Charset charset;
    private final String charsetName;
    private final Duration cacheDuration;
    private final CacheControl cacheControl;

    /**
     * Create a new {@link ServeResource} instance with the specified
     * {@link Configurations}.
     */
    @Inject
    public ServeResource(Configurations configurations)
    throws IOException {
        charset = Charset.forName(configurations.get("charset", UTF8.name()));
        charsetName = charset.name();

        manager = new ResourceManager(configurations.requireFile("root_path"), charset);

        minify = configurations.get("minify", false);

        cacheDuration = configurations.get("cache", Duration.ZERO);
        cacheControl = new CacheControl();
        cacheControl.setMaxAge((int) cacheDuration.getSeconds());
        cacheControl.setNoCache(Duration.ZERO.equals(cacheDuration));

        styleMediaType = new MediaType("text", "css").withCharset(charsetName);
        scriptMediaType = new MediaType("application", "javascript").withCharset(charsetName);
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
        Resource resource = manager.getResource(path);
        if ((resource == null) && path.endsWith(".css")) {
            path = path.substring(0, path.length() - 4) + ".less";
            resource = manager.getResource(path);
        }

        /* If the root is incorrect, log this, if not found, 404 it! */
        if (resource == null) throw new NotFoundException();
        final String fileName = resource.getFile().getName();

        /* Check and validated our cache */
        Entry cached = cache.computeIfPresent(resource,
                (r, entry) -> entry.resource.hasChanged() ? null : entry);

        /* If we have no cache, we *might* want to cache something */
        if (cached == null) {

            /* What to do, what to do? */
            if ((fileName.endsWith(".css") && minify) || fileName.endsWith(".less")) {

                /* Lessify CSS and cache */
                log.debug("Lessifying resource \"%s\"", fileName);
                cached = new Entry(resource,
                                   less.convert(resource.readString(), minify),
                                   styleMediaType);

            } else if (fileName.endsWith(".js") && minify) {

                /* Uglify JavaScript and cache */
                log.debug("Uglifying resource \"%s\"", fileName);
                cached = new Entry(resource,
                                   uglify.convert(resource.readString(), minify, minify),
                                   scriptMediaType);
            }

            /* Do we have anything to cache? */
            if (cached != null) {
                log.debug("Caching resource \"%s\"", fileName);
                cache.put(resource, cached);
            }
        }

        /* Prepare our basic response from either cache or file */
        final ResponseBuilder response = Response.ok();
        if (cached != null) {
            log.debug("Serving cached resource \"%s\"", fileName);
            response.entity(cached.contents)
                    .lastModified(new Date(resource.lastModifiedAt()))
                    .type(cached.type);

        } else {
            log.debug("Serving file resource \"%s\"", fileName);

            /* If text/* or application/javascript, append encoding */
            MediaType type = MediaTypes.get(fileName);
            if (type.getType().equals("text") || scriptMediaType.isCompatible(type)) {
                type = type.withCharset(charsetName);
            }

            /* Our file is served! */
            response.entity(resource.getFile())
                    .lastModified(new Date(resource.lastModifiedAt()))
                    .type(type);
        }

        /* Caching headers and build response */
        final Date expires = Date.from(Instant.now().plus(cacheDuration));
        return response.cacheControl(cacheControl)
                       .expires(expires)
                       .build();
    }

    /* ====================================================================== */

    private final class Entry {

        private final Resource resource;
        private final String contents;
        private final MediaType type;

        private Entry(Resource resource, String contents, MediaType type) {
            this.resource = notNull(resource);
            this.contents = notNull(contents);
            this.type = notNull(type);
        }
    }
}
