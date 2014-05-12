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

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.configurations.ConfigurationsBuilder;
import org.usrz.libs.httpd.ServerStarter;
import org.usrz.libs.logging.Log;
import org.usrz.libs.testing.AbstractTest;
import org.usrz.libs.testing.IO;
import org.usrz.libs.testing.NET;

public class ServeResourceTest extends AbstractTest {

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
        IO.copy("test.less", new File(directory, "test.less"));
        IO.copy("test.js",   new File(directory, "test.js"  ));

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
            builder.configure(serverConfig.strip("server"));
            builder.serveApp("/resources", (config) -> {
                config.register(ServeResource.class);
            }).withAppConfigurations(serverConfig.strip("resources"));

        });

        Thread.sleep(1000);

        testResource(directory, port, "test.less", IO.read("test.less"), ".foo{left:1px}".getBytes(), IO.read("test.min.css"));
        testResource(directory, port, "test.js",   IO.read("test.js"  ), "var a=1;".getBytes(),       IO.read("test.cm.min.js"));

    }

    private void testResource(File root, int port, String name, byte[] original, byte[] update, byte[] expected)
    throws Exception {
        final File file = new File(root, name);
        IO.copy(original, file);
        final long modified = file.lastModified();

        final URL url = new URL("http://127.0.0.1:" + port + "/resources/" + name);

        log.debug("Fetching %s with original content", url);
        assertRead(url, expected);

        /* Overwrite the file but preserve last modified */
        IO.copy(update, file);
        file.setLastModified(modified);

        log.debug("Fetching %s with modified content (same date)", url);
        assertRead(url, expected);

        /* "Touch" the resource for cache invalidation */
        file.setLastModified(modified + 10000);

        log.debug("Fetching %s with modified content and altered date", url);
        assertRead(url, update);

    }

    private void assertRead(URL url, byte[] expected)
    throws Exception {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        System.out.println("--> HTTP/" + connection.getResponseCode() + " " + connection.getResponseMessage());
        connection.getHeaderFields().forEach((name, values) -> {
            if (name != null) values.forEach((value) -> {
                System.out.println("--> " + name + ": " + value);
            });
        });

        final byte[] actual = IO.read(connection.getInputStream());
        assertNotNull(actual);
        assertEquals(new String(actual, UTF8), new String(expected, UTF8));
    }

}
