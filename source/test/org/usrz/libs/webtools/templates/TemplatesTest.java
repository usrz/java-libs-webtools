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
package org.usrz.libs.webtools.templates;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.configurations.ConfigurationsBuilder;
import org.usrz.libs.httpd.ServerStarter;
import org.usrz.libs.logging.Log;
import org.usrz.libs.testing.AbstractTest;
import org.usrz.libs.testing.IO;
import org.usrz.libs.testing.NET;
import org.usrz.libs.webtools.mustache.MustacheTemplateManager;


public class TemplatesTest extends AbstractTest {

    private static final Log log = new Log();
    private ServerStarter starter;
    private int port;

    @BeforeClass
    public void before()
    throws Exception {
        final File directory = IO.makeTempDir();
        IO.copy("Hello, {{>included}}!".getBytes(), new File(directory, "template.mustache"));
        IO.copy("{{name}}".getBytes(),              new File(directory, "included.mustache"));

        port = NET.serverPort();
        final Configurations serverConfig = new ConfigurationsBuilder()
                    .put("server.listener.port", port)
                    .put("server.listener.host", "127.0.0.1")
                    .put("server.listener.secure", false)
                    .put("resources.root_path", directory)
                    .put("resources.minify", true)
                    .put("resources.cache", "1 hour")
                    .build();

        starter = new ServerStarter().start((builder) -> {
            builder.install((binder) -> {
                binder.bind(TemplateManager.class)
                      .toInstance(new MustacheTemplateManager(directory));
            });

            builder.configure(serverConfig.strip("server"));
            builder.serveApp("/mustache", (config) -> {
                config.register(ViewBodyWriter.class);
                config.register(TestResource.class);
            });

        });

        Thread.sleep(1000);
    }

    @AfterClass(alwaysRun=true)
    public void after() {
        if (starter != null) starter.stop();
    }

    /* ====================================================================== */

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

    @Test
    public void runTestWithResponse()
    throws Exception {
        assertRead(new URL("http://127.0.0.1:" + port + "/mustache/withResponse"), "Hello, mustache.jax.response!");
    }

    @Test
    public void runTestWithView()
    throws Exception {
        assertRead(new URL("http://127.0.0.1:" + port + "/mustache/withView"),  "Hello, mustache.jax.view!");
    }

    @Test
    public void runTestWithViewOnly()
    throws Exception {
        assertRead(new URL("http://127.0.0.1:" + port + "/mustache/withViewOnly"),  "Hello, !");
    }

    @Test
    public void runTestWithAnnotation()
    throws Exception {
        assertRead(new URL("http://127.0.0.1:" + port + "/mustache/withAnnotation"), "Hello, mustache.jax.annotation!");
    }

    @Test
    public void runTestWithScope()
    throws Exception {
        assertRead(new URL("http://127.0.0.1:" + port + "/mustache/withScope"), "Hello, mustache.jax.scope!");
    }

    /* ====================================================================== */

    @Path("/")
    public static final class TestResource {

        @GET
        @Path("withResponse")
        public Response withResponse() {
            final Map<String, String> entity = Collections.singletonMap("name", "mustache.jax.response");
            return ViewResponse.ok().view("template.mustache").entity(entity).build();
        }

        @GET
        @Path("withView")
        public Response withView() {
            return ViewResponse.ok().view("template.mustache").with("name", "mustache.jax.view").build();
        }

        @GET
        @Path("withViewOnly")
        public Response withViewOnly() {
            return ViewResponse.ok().view("template.mustache").build();
        }

        @GET
        @Path("withAnnotation")
        @View("template.mustache")
        public Object withAnnotation() {
            return Collections.singletonMap("name", "mustache.jax.annotation");
        }

        @GET
        @Path("withScope")
        @View("template.mustache")
        public Scope withScope() {
            return Scope.builder().with("name", "mustache.jax.scope").build();
        }

    }
}
