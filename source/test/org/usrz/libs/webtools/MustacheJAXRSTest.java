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

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.configurations.ConfigurationsBuilder;
import org.usrz.libs.httpd.ServerStarter;
import org.usrz.libs.logging.Log;
import org.usrz.libs.testing.AbstractTest;
import org.usrz.libs.testing.IO;
import org.usrz.libs.testing.NET;
import org.usrz.libs.webtools.mustache.MustacheBodyWriter;
import org.usrz.libs.webtools.mustache.ReloadingMustacheFactory;
import org.usrz.libs.webtools.mustache.Template;
import org.usrz.libs.webtools.mustache.View;


public class MustacheJAXRSTest extends AbstractTest {

    private static final Log log = new Log();
    private ServerStarter starter;

    @AfterClass(alwaysRun=true)
    public void after() {
        if (starter != null) starter.stop();
    }

    @Test
    public void testServeResource()
    throws Exception {
        final File directory = IO.makeTempDir();
        IO.copy("Hello, {{>included}}!".getBytes(), new File(directory, "template.mustache"));
        IO.copy("{{name}}".getBytes(),              new File(directory, "included.mustache"));

        final int port = NET.serverPort();
        final Configurations serverConfig = new ConfigurationsBuilder()
                    .put("server.listener.port", port)
                    .put("server.listener.host", "127.0.0.1")
                    .put("server.listener.secure", false)
                    .put("resources.root_path", directory)
                    .put("resources.minify", true)
                    .put("resources.cache", "1 hour")
                    .build();

        starter = new ServerStarter().start((builder) -> {
            builder.install((consumer) -> {
                consumer.bind(ReloadingMustacheFactory.class).toInstance(new ReloadingMustacheFactory(directory));
            });

            builder.configure(serverConfig.strip("server"));
            builder.serveApp("/mustache", (config) -> {
                config.register(MustacheBodyWriter.class);
                config.register(TestResource.class);
            });

        });

        Thread.sleep(1000);

        assertRead(new URL("http://127.0.0.1:" + port + "/mustache/withResponse"),   "Hello, mustache.jax.response!");
        assertRead(new URL("http://127.0.0.1:" + port + "/mustache/withView"),       "Hello, mustache.jax.view!");
        assertRead(new URL("http://127.0.0.1:" + port + "/mustache/withAnnotation"), "Hello, mustache.jax.annotation!");
    }

    private void assertRead(URL url, String expected)
    throws Exception {
        log.debug("Fetching %s", url);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        System.out.println("--> HTTP/" + connection.getResponseCode() + " " + connection.getResponseMessage());
        connection.getHeaderFields().forEach((name, values) -> {
            if (name != null) values.forEach((value) -> {
                System.out.println("--> " + name + ": " + value);
            });
        });

        final byte[] actual = IO.read(connection.getInputStream());
        assertNotNull(actual);
        System.out.println("==> " + new String(actual));
        assertEquals(new String(actual), expected);
    }

    /* ====================================================================== */

    @Path("/")
    public static final class TestResource {

        @GET
        @Path("withResponse")
        public Response withResponse() {
            final Map<String, String> context = Collections.singletonMap("name", "mustache.jax.response");
            return Response.ok(new View("template.mustache", context)).build();
        }

        @GET
        @Path("withView")
        public View withView() {
            final Map<String, String> context = Collections.singletonMap("name", "mustache.jax.view");
            return new View("template.mustache", context);
        }

        @GET
        @Path("withAnnotation")
        @Template("template.mustache")
        public Object withAnnotation() {
            return Collections.singletonMap("name", "mustache.jax.annotation");
        }

    }
}
