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

import static org.usrz.libs.utils.Charsets.UTF8;

import java.net.HttpURLConnection;
import java.net.URL;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.configurations.ConfigurationsBuilder;
import org.usrz.libs.httpd.ServerStarter;
import org.usrz.libs.testing.AbstractTest;
import org.usrz.libs.testing.IO;
import org.usrz.libs.testing.NET;
import org.usrz.libs.webtools.exceptions.GeneralExceptionMapper;
import org.usrz.libs.webtools.exceptions.HtmlExceptionBodyWriter;
import org.usrz.libs.webtools.exceptions.JsonExceptionBodyWriter;

public class TestExceptionsAbstract extends AbstractTest {

    // ae6a9736-845a-4062-afeb-b20669d54e2a
    public static final String UUID_PATTERN = "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}";

    protected ServerStarter starter;
    protected int port;

    @BeforeClass(alwaysRun=true)
    protected final void before()
    throws Exception {
        port = NET.serverPort();
        final Configurations serverConfig = new ConfigurationsBuilder()
                    .put("server.listener.port", port)
                    .put("server.listener.host", "127.0.0.1")
                    .put("server.listener.secure", false)
                    .build();

        starter = new ServerStarter().start((builder) -> {
            builder.configure(serverConfig.strip("server"));
            builder.serveApp("/fail", (config) -> {
                config.register(FailResource.class);
                config.register(GeneralExceptionMapper.class);
                config.register(HtmlExceptionBodyWriter.class);
                config.register(JsonExceptionBodyWriter.class);
            });
        });

        Thread.sleep(1000);
    }

    @AfterClass(alwaysRun=true)
    protected final void after() {
        if (starter != null) starter.stop();
    }

    protected final void assertResponse(String path, String accept, String acceptableType, String acceptablePattern)
    throws Exception {
        URL url = new URL("http://127.0.0.1:" + port + path);
        log.info("Requesting \"%s\" with Accept \"%s\"", url, accept);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept", accept);
        connection.connect();

        System.out.println("--> HTTP/" + connection.getResponseCode() + " " + connection.getResponseMessage());
        connection.getHeaderFields().forEach((name, values) -> {
            if (name != null) values.forEach((value) -> {
                System.out.println("--> " + name + ": " + value);
            });
        });
        final String actual = new String(IO.read(connection.getErrorStream()), UTF8).trim();
        System.out.println(">>> " + actual);

        assertEquals(connection.getResponseCode(), 500, "Wrong status code");
        assertEquals(connection.getHeaderField("Content-Type"), acceptableType, "Wrong Content-Type header");

        assertTrue(actual.matches(acceptablePattern), "\nWrong response: " + actual + "\n       pattern: " + acceptablePattern + "\n");
    }

}
