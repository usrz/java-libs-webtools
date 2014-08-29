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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.configurations.ConfigurationsBuilder;
import org.usrz.libs.httpd.ServerStarter;
import org.usrz.libs.testing.AbstractTest;
import org.usrz.libs.testing.IO;
import org.usrz.libs.testing.NET;
import org.usrz.libs.webtools.resources.ServeResource;

public class ServeResourceTest extends AbstractTest {

    private ServerStarter starter;
    private File root;
    private int port;

    @BeforeClass(alwaysRun=true)
    public void before()
    throws Exception {
        root = IO.makeTempDir();

        if (port < 1024) port = NET.serverPort();
        final Configurations serverConfig = new ConfigurationsBuilder()
                    .put("server.listener.port", port)
                    .put("server.listener.host", "127.0.0.1")
                    .put("server.listener.secure", false)
                    .put("server.access_log.file", "/dev/stdout")
                    .put("server.access_log.format", "%{HH:mm:ss.SSS}t [-----] \"%r\" %s (%b bytes / %{milli}T ms)")
                    .put("server.access_log.synchronous", true)
                    .put("resources.root_path", root)
                    .put("resources.minify", true)
                    .put("resources.cache", "1 hour")
                    .put("resources.executor.core_pool_size", 10)
                    .put("resources.executor.maximum_pool_size", 50)
                    .put("resources.executor.notifier_threads", 5)
                    .put("resources.executor.keep_alive_time", "10 sec")
                    .put("resources.executor.queue_size", 10)
                    .put("resources.executor.executor_name", "ServeResourceTest")
                    .build();

        starter = new ServerStarter().start((builder) -> {
            builder.configure(serverConfig.strip("server"));
            builder.serveApp("/resources", (config) -> {
                config.register(ServeResource.class);
            }).withAppConfigurations(serverConfig.strip("resources"));

        });

        Thread.sleep(1000);
    }

    @AfterClass(alwaysRun=true)
    public void after() {
        if (starter != null) starter.stop();
    }

    @Test
    public void testResourceNotFound()
    throws Exception {
        assertEquals(openAndDumpHeaders(new URL("http://127.0.0.1:" + port + "/resources/foo.bar")).getResponseCode(), 404);
    }

    @Test
    public void testScriptResource()
    throws Exception {
        testResource(new File(root, "test.js"),   port, "test.js",   IO.read("test.js"  ), "var a=1;".getBytes(),       IO.read("test.cm.min.js"));
    }

    @Test
    public void testCssResource()
    throws Exception {
        testResource(new File(root, "test.less"), port, "test.css",  IO.read("test.less"), ".foo{left:1px}".getBytes(), IO.read("test.min.css"));
    }

    @Test(priority=1)
    public void testLessResource()
    throws Exception {
        testResource(new File(root, "test.less"), port, "test.less", IO.read("test.less"), ".foo{left:1px}".getBytes(), IO.read("test.min.css"));
    }

    @Test(priority=2)
    public void testLessResourceImport()
    throws Exception {
        /* Restore "test.less" before testing import */
        IO.copy(IO.read("test.less"), new File(root, "test.less"));
        testResource(new File(root, "import.less"), port, "import.less", IO.read("import.less"), ".foo{left:1px}".getBytes(), IO.read("import.min.css"));
    }

    @Test
    public void testJsonResource()
    throws Exception {
        testResource(new File(root, "test.json"), port, "test.json", IO.read("test.json"), "{\"foo\":\"baz\"}".getBytes(), "{\"foo\":\"bar\"}".getBytes());
    }

    @Test
    public void testBinaryResource()
    throws Exception {
        final byte[] data = IO.read("test.bin");
        IO.copy(data, new File(root, "test.bin"));
        assertRead(new URL("http://127.0.0.1:" + port + "/resources/test.bin"), data, "Binary resource", true);
    }

    /* ====================================================================== */

    private void testResource(File file, int port, String name, byte[] original, byte[] update, byte[] expected)
    throws Exception {
        IO.copy(original, file);
        final long modified = file.lastModified();

        final URL url = new URL("http://127.0.0.1:" + port + "/resources/" + name);

        log.debug("Fetching %s with original content", url);
        assertRead(url, expected, "Original content for " + url, false);

        /* Overwrite the file but preserve last modified */
        IO.copy(update, file);
        file.setLastModified(modified);

        log.debug("Fetching %s with modified content (same date)", url);
        assertRead(url, expected, "Modified content (same date) for " + url, false);

        /* "Touch" the resource for cache invalidation */
        file.setLastModified(modified + 10000);

        log.debug("Fetching %s with modified content and altered date", url);
        assertRead(url, update, "Modified content and date for " + url, false);

    }

    private void assertRead(URL url, byte[] expected, String message, boolean binary)
    throws Exception {
        final HttpURLConnection connection = openAndDumpHeaders(url);

        final byte[] actual = IO.read(connection.getInputStream());
        assertNotNull(actual, message);
        if (!binary) {
            message = message + "\n  Expected: " + new String(expected)
                              + "\n    Actual: " + new String(actual)
                              + "\n   Details:";
        }
        assertEquals(actual, expected, message);
    }

    private HttpURLConnection openAndDumpHeaders(URL url)
    throws Exception {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        System.out.println("--> HTTP/" + connection.getResponseCode() + " " + connection.getResponseMessage());
        connection.getHeaderFields().forEach((name, values) -> {
            if (name != null) values.forEach((value) -> {
                System.out.println("--> " + name + ": " + value);
            });
        });
        return connection;
    }

    /* ====================================================================== */

    public static void main(String[] args) throws Exception {
        final ServeResourceTest test = new ServeResourceTest();
        test.port = 55555; // set the port!
        test.before();

        IO.copy("test.bin",  new File(test.root, "test.bin"));
        IO.copy("test.less", new File(test.root, "test.less"));
        IO.copy("test.js",   new File(test.root, "test.js"));
        System.out.println("Files copied at:");
        System.out.println(" - " + test.root);
        System.out.println("Try benchmarking:");
        System.out.println(" - http://127.0.0.1:" + test.port + "/resources/test.bin");
        System.out.println(" - http://127.0.0.1:" + test.port + "/resources/test.css");
        System.out.println(" - http://127.0.0.1:" + test.port + "/resources/test.js");
        Thread.sleep(Long.MAX_VALUE);
    }


}
