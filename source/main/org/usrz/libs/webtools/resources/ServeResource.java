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
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.logging.Log;
import org.usrz.libs.utils.concurrent.KeyedExecutor;
import org.usrz.libs.utils.concurrent.NotifyingFuture;
import org.usrz.libs.utils.concurrent.SimpleExecutorProvider;
import org.usrz.libs.webtools.lesscss.LessCSS;
import org.usrz.libs.webtools.uglifyjs.UglifyJS;
import org.usrz.libs.webtools.utils.MediaTypes;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

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

    private static final Response NOT_FOUND = Response.status(Status.NOT_FOUND).build();

    private final MediaType jsonMediaType;
    private final MediaType styleMediaType;
    private final MediaType scriptMediaType;

    private final Log xlog = new Log();

    private final JsonFactory json;
    private final LessCSS lxess = new LessCSS();
    private final UglifyJS uglify = new UglifyJS();

    private final ConcurrentMap<Resource, Entry> cache = new ConcurrentHashMap<>();

    private final KeyedExecutor<String> executor;
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

        jsonMediaType = new MediaType("application", "json").withCharset(charsetName);
        styleMediaType = new MediaType("text", "css").withCharset(charsetName);
        scriptMediaType = new MediaType("application", "javascript").withCharset(charsetName);

        executor = new KeyedExecutor<String>(SimpleExecutorProvider.create(configurations.strip("executor")));

        /* Our Json factory, able to read all sorts of weird schtuff */
        json = new JsonFactory()
                /* Factory features */
                .disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES)
                .disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
                /* Parser features */
                .enable(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS)
                .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
                .enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS)
                .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
                .disable(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS)
                .disable(JsonParser.Feature.ALLOW_YAML_COMMENTS)
                .disable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                /* Generator features */
                .disable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT)
                .enable(JsonGenerator.Feature.ESCAPE_NON_ASCII)
                .enable(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM)
                .enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES)
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                .disable(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS)
                .disable(JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION)
                .disable(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS);
    }

    /**
     * Serve the specified resource (either {@code GET} or {@code HEAD}).
     */
    @GET
    @Path("{resource:.*}")
    public void serve(@Suspended AsyncResponse asyncResponse, @PathParam("resource") String path) {
        final NotifyingFuture<Response> future;
        try {
            /* Schedule our request generation */
            future = executor.call(path, () -> produce(path));
            xlog.trace("\"%s\": AsyncResponse %s using future %s", path, asyncResponse, future);

        } catch (RejectedExecutionException exception) {
            /* Too many requests in the queue */
            xlog.warn("\"%s\": Canceling AsyncResponse %s", path, asyncResponse);
            asyncResponse.cancel();
            return;

        } catch (Throwable exception) {
            /* Some other weird exception */
            xlog.warn("\"%s\": Failing AsyncResponse %s", path, asyncResponse);
            asyncResponse.resume(exception);
            return;
        }

        /* Notify us of completion */
        future.withConsumer((responseFuture) -> {
            try {
                /* Get a hold on the shared response to duplicate */
                final Response sharedResponse = responseFuture.get();

                /* ===================  SUPER  IMPORTANT  =================== *
                 * Always clone the response *ALWAYS*... This is not because  *
                 * of the potential NOT_FOUND shared above, but Jersey keeps  *
                 * something in the Response itself, and submitting the same  *
                 * instance multiple times to different AsyncRespons(e) makes *
                 * the whole thing fail (Grizzly gets confused on streams)... *
                 * ===================  super  important  =================== */
                final Response response = Response.fromResponse(sharedResponse).build();

                /* Complete the response */
                asyncResponse.resume(response);
                xlog.trace("\"%s\": Completed AsyncResponse %s", path, asyncResponse);

            } catch (Exception exception) {
                xlog.trace("\"%s\": Exception processing AsyncResponse %s", path, asyncResponse);
                asyncResponse.resume(exception);
                return;
            }
        });
    }

    /* ====================================================================== */

    /* Deferred proces to create a Response from a path */
    private Response produce(String path)
    throws Exception {

        /* Basic check for null/empty path */
        if ((path == null) || (path.length() == 0)) return NOT_FOUND;

        /* Get our resource file, potentially a ".less" file for CSS */
        Resource resource = manager.getResource(path);
        if ((resource == null) && path.endsWith(".css")) {
            path = path.substring(0, path.length() - 4) + ".less";
            resource = manager.getResource(path);
        }

        /* If the root is incorrect, log this, if not found, 404 it! */
        if (resource == null) return NOT_FOUND;

        /* Ok, we have a resource on disk, this can be potentially long ... */
        final String fileName = resource.getFile().getName();

        /* Check and validated our cache */
        Entry cached = cache.computeIfPresent(resource,
                (r, entry) -> entry.resource.hasChanged() ? null : entry);

        /* If we have no cache, we *might* want to cache something */
        if (cached == null) {

            /* What to do, what to do? */
            if ((fileName.endsWith(".css") && minify) || fileName.endsWith(".less")) {

                /* Lessify CSS and cache */
                xlog.debug("Lessifying resource \"%s\"", fileName);
                cached = new Entry(resource,
                                   lxess.convert(resource, minify),
                                   styleMediaType);

            } else if (fileName.endsWith(".js") && minify) {

                /* Uglify JavaScript and cache */
                xlog.debug("Uglifying resource \"%s\"", fileName);
                cached = new Entry(resource,
                                   uglify.convert(resource.readString(), minify, minify),
                                   scriptMediaType);

            } else if (fileName.endsWith(".json")) {

                /* Strip comments and normalize JSON */
                xlog.debug("Normalizing JSON resource \"%s\"", fileName);

                /* All to do with Jackson */
                final Reader reader = resource.read();
                final StringWriter writer = new StringWriter();
                final JsonParser parser = json.createParser(reader);
                final JsonGenerator generator = json.createGenerator(writer);

                /* Not minifying? Means pretty printing! */
                if (!minify) generator.useDefaultPrettyPrinter();

                /* Get our schtuff through the pipeline */
                parser.nextToken();
                generator.copyCurrentStructure(parser);
                generator.flush();
                generator.close();
                reader.close();

                /* Cached results... */
                cached = new Entry(resource, writer.toString(), jsonMediaType);

            }

            /* Do we have anything to cache? */
            if (cached != null) {
                xlog.debug("Caching resource \"%s\"", fileName);
                cache.put(resource, cached);
            }
        }

        /* Prepare our basic response from either cache or file */
        final ResponseBuilder response = Response.ok();
        if (cached != null) {

            /* Response from cache */
            xlog.trace("Serving cached resource \"%s\"", fileName);
            response.entity(cached.contents)
                    .lastModified(new Date(resource.lastModifiedAt()))
                    .type(cached.type);
        } else {

            /* Response from a file */
            xlog.trace("Serving file-based resource \"%s\"", fileName);

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
