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
package org.usrz.libs.webtools.exceptions;

import static org.usrz.libs.utils.Charsets.UTF8;

import java.net.HttpURLConnection;
import java.net.URL;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.usrz.libs.httpd.ServerStarter;
import org.usrz.libs.testing.AbstractTest;
import org.usrz.libs.testing.IO;

public abstract class TestWithServer extends AbstractTest {

    protected static final String UUID_PATTERN = "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}";

    protected ServerStarter starter;
    protected int port;

    @BeforeClass(alwaysRun=true)
    public abstract void before()
    throws Exception;

    @AfterClass(alwaysRun=true)
    public final void after() {
        if (starter != null) starter.stop();
    }

    /* ====================================================================== */

    protected void assertResponse(String path, String accept, int acceptableStatus, String acceptableType, String acceptablePattern)
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

        assertEquals(connection.getResponseCode(), acceptableStatus, "Wrong status code");
        assertEquals(connection.getHeaderField("Content-Type"), acceptableType, "Wrong Content-Type header");

        assertTrue(actual.matches(acceptablePattern), "\nWrong response: " + actual + "\n       pattern: " + acceptablePattern + "\n");
    }

}
