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

import java.io.File;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.configurations.ConfigurationsBuilder;
import org.usrz.libs.httpd.ServerStarter;
import org.usrz.libs.testing.IO;
import org.usrz.libs.testing.NET;
import org.usrz.libs.webtools.templates.ReloadingMustacheFactory;
import org.usrz.libs.webtools.templates.TemplateFactory;

public class TemplateExceptionBodyWriterTest extends TestWithServer {

    @Override
    @BeforeClass(alwaysRun=true)
    public void before()
    throws Exception {
        final File root = IO.makeTempDir();

        port = NET.serverPort();
        final Configurations serverConfig = new ConfigurationsBuilder()
                    .put("server.listener.port", port)
                    .put("server.listener.host", "127.0.0.1")
                    .put("server.listener.secure", false)
                    .build();

        starter = new ServerStarter().start((builder) -> {
            builder.install((binder) -> binder.bind(TemplateFactory.class).toInstance(new ReloadingMustacheFactory(root)));
            builder.configure(serverConfig.strip("server"));
            builder.serveApp("/fail", (config) -> {
                config.register(FailResource.class);
                config.register(GeneralExceptionMapper.class);
                config.register(JsonExceptionBodyWriter.class);
                // register both HtmlExceptionBodyWriter and TemplateExceptionBodyWriter
                // register HtmlExceptionBodyWriter before and after TemplateExceptionBodyWriter
                // no matter what, the priority of TemplateExceptionBodyWriter will
                // make it work always!
                config.register(HtmlExceptionBodyWriter.class);
                config.register(TemplateExceptionBodyWriter.class);
                config.register(HtmlExceptionBodyWriter.class);
            });
        });

        Thread.sleep(1000);
    }

    /* ====================================================================== */

    @Test
    public void testHtmlWithHtmlPriority()
    throws Exception {
        assertResponse("/fail/html?p=%3Cdiv+class=%22foobar%22%2F%3E-%e6%9d%b1%e4%ba%ac", "text/html;q=0.9,application/json;q=0.1", 500, "text/html; charset=UTF-8",
                       "<!DOCTYPE html><html><head><title>Error 500: Internal Server Error</title></head><body><h1>Error 500: Internal Server Error</h1>"
                       + "<dl><dt>Message:</dt><dd>HTML Error Parameter: &lt;div class=&quot;foobar&quot;/&gt;-\u6771\u4EAC</dd><dt>Reference:</dt><dd>"
                       + UUID_PATTERN + "</dd><dt>Exception:</dt><dd>class java.lang.RuntimeException</dd></dl></body></html>");
    }

    @Test
    public void testHtmlWithJsonPriority()
    throws Exception {
        assertResponse("/fail/html?p=%3Cdiv+class=%22foobar%22%2F%3E-%e6%9d%b1%e4%ba%ac", "text/html;q=0.1,application/json;q=0.9", 500, "text/html; charset=UTF-8",
                       "<!DOCTYPE html><html><head><title>Error 500: Internal Server Error</title></head><body><h1>Error 500: Internal Server Error</h1>"
                       + "<dl><dt>Message:</dt><dd>HTML Error Parameter: &lt;div class=&quot;foobar&quot;/&gt;-\u6771\u4EAC</dd><dt>Reference:</dt><dd>"
                       + UUID_PATTERN + "</dd><dt>Exception:</dt><dd>class java.lang.RuntimeException</dd></dl></body></html>");
    }

    @Test
    public void testJsonWithHtmlPriority()
    throws Exception {
        assertResponse("/fail/json?p=foo%22bar%22baz-%e6%9d%b1%e4%ba%ac", "text/html;q=0.9,application/json;q=0.1", 500, "application/json; charset=UTF-8",
                       "\\{\"status_code\":500,\"status_reason\":\"Internal Server Error\",\"reference\":\"" + UUID_PATTERN + "\",\"message\":\"JSON Error Parameter: foo\\\\\"bar\\\\\"baz-\u6771\u4EAC\",\"exception\":\"java.lang.RuntimeException\"\\}");
    }

    @Test
    public void testJsonWithJsonPriority()
    throws Exception {
        assertResponse("/fail/json?p=foo%22bar%22baz-%e6%9d%b1%e4%ba%ac", "text/html;q=0.1,application/json;q=0.9", 500, "application/json; charset=UTF-8",
                       "\\{\"status_code\":500,\"status_reason\":\"Internal Server Error\",\"reference\":\"" + UUID_PATTERN + "\",\"message\":\"JSON Error Parameter: foo\\\\\"bar\\\\\"baz-\u6771\u4EAC\",\"exception\":\"java.lang.RuntimeException\"\\}");
    }

    @Test
    public void testNegotiateWithHtmlPriority()
    throws Exception {
        assertResponse("/fail/negotiate?p=%3Cdiv+class=%22foobar%22%2F%3E-%e6%9d%b1%e4%ba%ac", "text/html;q=0.9,application/json;q=0.1", 500, "text/html; charset=UTF-8",
                       "<!DOCTYPE html><html><head><title>Error 500: Internal Server Error</title></head><body><h1>Error 500: Internal Server Error</h1>"
                       + "<dl><dt>Message:</dt><dd>Negotiated Error Parameter: &lt;div class=&quot;foobar&quot;/&gt;-\u6771\u4EAC</dd><dt>Reference:</dt><dd>"
                       + UUID_PATTERN + "</dd><dt>Exception:</dt><dd>class java.lang.RuntimeException</dd></dl></body></html>");
    }

    @Test
    public void testNegotiateWithJsonPriority()
    throws Exception {
        assertResponse("/fail/negotiate?p=foo%22bar%22baz-%e6%9d%b1%e4%ba%ac", "text/html;q=0.1,application/json;q=0.9", 500, "application/json; charset=UTF-8",
                       "\\{\"status_code\":500,\"status_reason\":\"Internal Server Error\",\"reference\":\"" + UUID_PATTERN + "\",\"message\":\"Negotiated Error Parameter: foo\\\\\"bar\\\\\"baz-\u6771\u4EAC\",\"exception\":\"java.lang.RuntimeException\"\\}");
    }

    @Test
    public void testTemplatedWithHtmlPriority()
    throws Exception {
        assertResponse("/fail/template?p=%3Cdiv+class=%22foobar%22%2F%3E-%e6%9d%b1%e4%ba%ac", "text/html;q=0.9,application/json;q=0.1", 500, "text/html; charset=UTF-8",
                       "<!DOCTYPE html><html><head><title>Error 500: Internal Server Error</title></head><body><h1>Error 500: Internal Server Error</h1>"
                       + "<dl><dt>Message:</dt><dd>Templated Error Parameter: &lt;div class=&quot;foobar&quot;/&gt;-\u6771\u4EAC</dd><dt>Reference:</dt><dd>"
                       + UUID_PATTERN + "</dd><dt>Exception:</dt><dd>class org.usrz.libs.webtools.exceptions.TemplatedException</dd></dl></body></html>");
    }

    @Test
    public void testTemplatedWithJsonPriority()
    throws Exception {
        assertResponse("/fail/template?p=foo%22bar%22baz-%e6%9d%b1%e4%ba%ac", "text/html;q=0.1,application/json;q=0.9", 500, "application/json; charset=UTF-8",
                       "\\{\"status_code\":500,\"status_reason\":\"Internal Server Error\",\"reference\":\"" + UUID_PATTERN + "\",\"message\":\"Templated Error Parameter: foo\\\\\"bar\\\\\"baz-\u6771\u4EAC\",\"exception\":\"org.usrz.libs.webtools.exceptions.TemplatedException\"\\}");
    }

}
