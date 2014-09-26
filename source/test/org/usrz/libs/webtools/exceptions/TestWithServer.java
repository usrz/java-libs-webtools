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

import static java.util.regex.Pattern.DOTALL;
import static org.usrz.libs.utils.Charsets.UTF8;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    protected final class Response {

        public final int status;
        public final String contentType;
        public final String content;

        private Response(int status, String contentType, String content) {
            this.status = status;
            this.contentType = contentType;
            this.content = content;
        }

        public Response assertStatus(int status) {
            assertEquals(this.status, status, "Wrong status code");
            return this;
        }

        public Response assertContentType(String contentType) {
            assertEquals(this.contentType, contentType, "Wrong content type");
            return this;
        }

        public Response assertMatch(String contentPattern) {
            final Pattern pattern = Pattern.compile(contentPattern, DOTALL);
            final Matcher matcher = pattern.matcher(content);
            assertTrue(matcher.find(), "\nWrong response for pattern " + pattern + "\n" + content);
            return this;
        }

        public Response assertNotMatch(String contentPattern) {
            final Pattern pattern = Pattern.compile(contentPattern, DOTALL);
            final Matcher matcher = pattern.matcher(content);
            assertFalse(matcher.find(), "\nWrong response for non-matching pattern " + pattern + "\n" + content);
            return this;
        }
    }

    /* ====================================================================== */

    protected Response request(String path)
    throws Exception {
        return request(path, null);
    }

    @SuppressWarnings("resource")
    protected Response request(String path, String accept)
    throws Exception {
        URL url = new URL("http://127.0.0.1:" + port + path);
        log.info("Requesting \"%s\" with Accept \"%s\"", url, accept);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (accept != null) connection.setRequestProperty("Accept", accept);
        connection.setInstanceFollowRedirects(false);
        connection.connect();

        System.out.println("--> HTTP/" + connection.getResponseCode() + " " + connection.getResponseMessage());
        connection.getHeaderFields().forEach((name, values) -> {
            if (name != null) values.forEach((value) -> {
                System.out.println("--> " + name + ": " + value);
            });
        });

        InputStream stream = connection.getErrorStream();
        if (stream == null) stream = connection.getInputStream();

        final String actual = new String(IO.read(stream), UTF8);
        stream.close();

        System.out.println(">>> " + actual.replace("\n", "\n>>> "));
        return new Response(connection.getResponseCode(),
                            connection.getHeaderField("Content-Type"),
                            actual);
    }


    protected void assertResponse(String path, String accept, int acceptableStatus, String acceptableType, String acceptablePattern)
    throws Exception {
        request(path, accept)
                .assertStatus(acceptableStatus)
                .assertContentType(acceptableType)
                .assertMatch(acceptablePattern);
    }

}
